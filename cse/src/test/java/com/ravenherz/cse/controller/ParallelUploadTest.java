package com.ravenherz.cse.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParallelUploadTest {

    @Test
    void assemblesPartsInOrder() throws Exception {
        ParallelUpload uploads = new ParallelUpload();
        byte[] first = "hello ".getBytes(StandardCharsets.UTF_8);
        byte[] second = "world".getBytes(StandardCharsets.UTF_8);
        assertNull(uploads.accept("track", 1, 2, first.length + second.length,
                new MockMultipartFile("file", "b", "application/octet-stream", second)));
        var assembled = uploads.accept("track", 0, 2, first.length + second.length,
                new MockMultipartFile("file", "a", "application/octet-stream", first));
        assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(assembled));
        uploads.cancel("track");
    }
}
