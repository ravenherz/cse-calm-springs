package com.ravenherz.optideployer;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UploadFlowTest {

    @Test
    void splitsIntoEightPartsThatSumToTheFile() {
        assertEquals(0, sum(PartPlan.lengths(0)));
        assertEquals(7, sum(PartPlan.lengths(7)));
        assertEquals(8, sum(PartPlan.lengths(8)));
        assertEquals(11, sum(PartPlan.lengths(11)));
        long[] eleven = PartPlan.lengths(11);
        assertEquals(2, eleven[0]);
        assertEquals(2, eleven[1]);
        assertEquals(2, eleven[2]);
        assertEquals(1, eleven[3]);
    }

    @Test
    void readsTheExampleAndNormalizesTheContext() throws Exception {
        String json = Files.readString(Path.of("build-info/optideploy.json.example"));
        OptiConfig config = OptiConfig.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        assertTrue(config.context().startsWith("/"));
        assertTrue(config.matches("00000000-0000-0000-0000-000000000000"));
        assertFalse(config.matches("other"));
        assertFalse(config.managerUrl().endsWith("/"));
    }

    @Test
    void missingFieldFailsFast() {
        byte[] json = "{\"secret\":\"x\"}".getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalStateException.class, () -> OptiConfig.read(new ByteArrayInputStream(json)));
    }

    @Test
    void deployUrlPointsTomcatAtTheLocalWar() {
        String url = TomcatManager.deployUrl(
                "http://127.0.0.1:8080/manager/text",
                "/rhz-we",
                Path.of("D:/var/cse/staging/cse.war"));
        assertTrue(url.startsWith("http://127.0.0.1:8080/manager/text/deploy?"));
        assertTrue(url.contains("update=true"));
        assertTrue(url.contains("path=%2Frhz-we"));
        assertTrue(url.contains("war="));
        assertTrue(url.contains("cse.war"));
    }

    @Test
    void eightPartsAssembleInOrderAndDeployOnce() throws Exception {
        Path dir = Files.createTempDirectory("opti");
        Path war = dir.resolve("app.war");
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<byte[]> seen = new AtomicReference<>();
        UploadService service = new UploadService(config(war), path -> {
            calls.incrementAndGet();
            seen.set(Files.readAllBytes(path));
            return "OK - Deployed application at context path [/rhz-we]";
        });
        byte[] payload = "hello war!!".getBytes(StandardCharsets.UTF_8);
        List<UploadResult> results = send(service, "secret", "upload-1", payload);
        assertEquals(1, results.stream().filter(r -> r.status() == 200).count());
        assertEquals(7, results.stream().filter(r -> r.status() == 204).count());
        assertEquals(1, calls.get());
        assertArrayEquals(payload, seen.get());
        assertArrayEquals(payload, Files.readAllBytes(war));
        assertTrue(results.stream().filter(r -> r.status() == 200).findFirst().orElseThrow().body().startsWith("OK"));
    }

    @Test
    void wrongSecretDoesNotWriteTheWar() throws Exception {
        Path dir = Files.createTempDirectory("opti");
        Path war = dir.resolve("app.war");
        AtomicInteger calls = new AtomicInteger();
        UploadService service = new UploadService(config(war), path -> {
            calls.incrementAndGet();
            return "OK";
        });
        UploadResult result = service.accept("nope", "upload-1", 0, 1, new ByteArrayInputStream(new byte[]{1}));
        assertEquals(403, result.status());
        assertEquals(0, calls.get());
        assertFalse(Files.exists(war));
        assertFalse(Files.exists(dir.resolve("app.war.parts")));
    }

    @Test
    void mismatchedPartLengthDoesNotDeploy() throws Exception {
        Path war = Files.createTempDirectory("opti").resolve("app.war");
        AtomicInteger calls = new AtomicInteger();
        UploadService service = new UploadService(config(war), path -> {
            calls.incrementAndGet();
            return "OK";
        });
        List<UploadResult> results = new ArrayList<>();
        for (int i = 0; i < PartPlan.STREAMS; i++) {
            results.add(service.accept("secret", "upload-1", i, 8, new ByteArrayInputStream(new byte[]{1, 2})));
        }
        assertEquals(1, results.stream().filter(r -> r.status() == 400).count());
        assertEquals(0, calls.get());
        assertFalse(Files.exists(war));
    }

    @Test
    void aSecondUploadWaitsUntilTheDeployFinishes() throws Exception {
        Path war = Files.createTempDirectory("opti").resolve("app.war");
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        UploadService service = new UploadService(config(war), path -> {
            started.countDown();
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new java.io.IOException("deploy wait timed out");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new java.io.IOException(e);
            }
            return "OK - Deployed";
        });
        byte[] payload = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<List<UploadResult>> first = pool.submit(() -> send(service, "secret", "upload-1", payload));
            assertTrue(started.await(10, TimeUnit.SECONDS));
            UploadResult conflict = service.accept("secret", "upload-2", 0, 1, new ByteArrayInputStream(new byte[]{9}));
            assertEquals(409, conflict.status());
            release.countDown();
            List<UploadResult> results = first.get(10, TimeUnit.SECONDS);
            assertEquals(1, results.stream().filter(r -> r.status() == 200).count());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void filterRejectsAWrongQuerySecretBeforeTheBody() throws Exception {
        SecretFilter filter = new SecretFilter(config(Path.of("unused.war")));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/optideployer/upload-and-deploy/");
        request.setQueryString("secret-uuid=nope");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void filterPassesTheMatchingSecret() throws Exception {
        OptiConfig config = config(Path.of("unused.war"));
        SecretFilter filter = new SecretFilter(config);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/optideployer/upload-and-deploy/");
        request.setQueryString("secret-uuid=" + config.secret());
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void uploadEndpointReadsTheQuerySecretAndThePart() throws Exception {
        UploadService service = mock(UploadService.class);
        when(service.accept(eq("uuid"), eq("up"), eq(3), eq(9L), any())).thenReturn(UploadResult.stored());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new UploadController(service)).build();
        mvc.perform(multipart("/upload-and-deploy/")
                        .file(new MockMultipartFile("file", "cse.war", "application/octet-stream", new byte[]{1}))
                        .queryParam("secret-uuid", "uuid")
                        .param("uploadId", "up")
                        .param("partIndex", "3")
                        .param("totalSize", "9"))
                .andExpect(status().isNoContent());
        verify(service).accept(eq("uuid"), eq("up"), eq(3), eq(9L), any());
    }

    private static List<UploadResult> send(UploadService service, String secret, String uploadId, byte[] payload)
            throws Exception {
        long[] lens = PartPlan.lengths(payload.length);
        List<UploadResult> results = new ArrayList<>();
        int offset = 0;
        for (int i = 0; i < PartPlan.STREAMS; i++) {
            int length = (int) lens[i];
            byte[] slice = new byte[length];
            System.arraycopy(payload, offset, slice, 0, length);
            offset += length;
            results.add(service.accept(secret, uploadId, i, payload.length, new ByteArrayInputStream(slice)));
        }
        return results;
    }

    private static OptiConfig config(Path war) {
        return new OptiConfig(
                "secret",
                "http://127.0.0.1:8080/optideployer/upload-and-deploy/",
                war.toString(),
                "/rhz-we",
                "http://127.0.0.1:8080/manager/text",
                "admin",
                "admin");
    }

    private static long sum(long[] values) {
        long sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum;
    }
}
