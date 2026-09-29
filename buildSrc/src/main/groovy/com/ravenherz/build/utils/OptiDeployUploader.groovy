package com.ravenherz.build.utils

import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Splits a WAR into eight parts and posts them together.
 * The length split matches {@code com.ravenherz.optideployer.PartPlan}.
 */
class OptiDeployUploader {

    static void upload(String uploadUrl, String secret, File warFile) {
        if (!warFile.isFile()) {
            throw new IllegalStateException('WAR not found: ' + warFile)
        }
        if (!secret) {
            throw new IllegalStateException('.cse-deployment.yml is missing secret for cse-optideployer')
        }
        if (!uploadUrl) {
            throw new IllegalStateException('.cse-deployment.yml did not produce an upload URL')
        }
        String base = uploadUrl
        if (base.endsWith('?')) {
            base = base.substring(0, base.length() - 1)
        }
        if (!base.endsWith('/')) {
            base = base + '/'
        }
        URL url = URI.create(base).toURL()
        long size = warFile.length()
        long[] lens = lengths(size)
        String uploadId = UUID.randomUUID().toString()
        System.setProperty('http.maxConnections', '8')
        println "Uploading ${warFile.name} (${size} bytes) in 8 parts to ${base}"

        ExecutorService pool = Executors.newFixedThreadPool(8)
        List<Future<PartResult>> futures = []
        try {
            long offset = 0L
            for (int i = 0; i < 8; i++) {
                int index = i
                long partOffset = offset
                long partLength = lens[i]
                offset += partLength
                futures.add(pool.submit(new Callable<PartResult>() {
                    @Override
                    PartResult call() {
                        return post(url, secret, uploadId, index, size, warFile, partOffset, partLength)
                    }
                }))
            }
            List<PartResult> results = []
            for (Future<PartResult> future : futures) {
                try {
                    results.add(future.get())
                } catch (ExecutionException e) {
                    throw new IllegalStateException('part upload failed: ' + e.cause.message, e.cause)
                }
            }
            List<PartResult> deployed = results.findAll { it.status == 200 }
            List<PartResult> stored = results.findAll { it.status == 204 }
            if (deployed.size() != 1 || stored.size() != 7) {
                String detail = results.collect { it.status + ' ' + it.body }.join('\n')
                throw new IllegalStateException('optideploy failed:\n' + detail)
            }
            String body = deployed[0].body == null ? '' : deployed[0].body.trim()
            if (!body.startsWith('OK')) {
                throw new IllegalStateException(body)
            }
            println body
        } finally {
            pool.shutdownNow()
        }
    }

    /** Same split as com.ravenherz.optideployer.PartPlan.lengths. */
    static long[] lengths(long size) {
        long[] lens = new long[8]
        long base = size.intdiv(8)
        long extra = size % 8
        for (int i = 0; i < 8; i++) {
            lens[i] = base + (i < extra ? 1L : 0L)
        }
        return lens
    }

    private static PartResult post(URL url, String secret, String uploadId, int index, long totalSize, File war, long offset, long length) {
        String boundary = '----opti' + UUID.randomUUID().toString().replace('-', '')
        HttpURLConnection conn = (HttpURLConnection) url.openConnection()
        conn.setRequestMethod('POST')
        conn.setDoOutput(true)
        conn.setUseCaches(false)
        conn.setInstanceFollowRedirects(false)
        conn.setChunkedStreamingMode(1024 * 1024)
        conn.setConnectTimeout(30000)
        conn.setReadTimeout(15 * 60 * 1000)
        conn.setRequestProperty('Connection', 'close')
        conn.setRequestProperty('X-Cse-Deploy-Secret', secret)
        conn.setRequestProperty('Content-Type', 'multipart/form-data; boundary=' + boundary)
        OutputStream out = conn.getOutputStream()
        try {
            writeField(out, boundary, 'uploadId', uploadId)
            writeField(out, boundary, 'partIndex', String.valueOf(index))
            writeField(out, boundary, 'totalSize', String.valueOf(totalSize))
            byte[] header = ('--' + boundary + '\r\n'
                    + 'Content-Disposition: form-data; name="file"; filename="cse.war"\r\n'
                    + 'Content-Type: application/octet-stream\r\n\r\n').getBytes('UTF-8')
            out.write(header)
            writeSlice(out, war, offset, length)
            out.write(('\r\n--' + boundary + '--\r\n').getBytes('UTF-8'))
        } finally {
            out.close()
        }
        int status = conn.getResponseCode()
        String body = ''
        try {
            InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream()
            if (stream != null) {
                body = stream.getText('UTF-8')
                stream.close()
            }
        } catch (IOException ignored) {
            body = ''
        }
        println "part ${index} -> ${status}"
        conn.disconnect()
        return new PartResult(status: status, body: body)
    }

    private static void writeField(OutputStream out, String boundary, String name, String value) {
        String part = '--' + boundary + '\r\nContent-Disposition: form-data; name="' + name + '"\r\n\r\n' + value + '\r\n'
        out.write(part.getBytes('UTF-8'))
    }

    private static void writeSlice(OutputStream out, File war, long offset, long length) {
        RandomAccessFile raf = new RandomAccessFile(war, 'r')
        try {
            raf.seek(offset)
            byte[] buf = new byte[1024 * 1024]
            long left = length
            while (left > 0L) {
                int n = raf.read(buf, 0, (int) Math.min((long) buf.length, left))
                if (n < 0) {
                    break
                }
                out.write(buf, 0, n)
                left -= n
            }
        } finally {
            raf.close()
        }
    }

    static class PartResult {
        int status
        String body
    }
}
