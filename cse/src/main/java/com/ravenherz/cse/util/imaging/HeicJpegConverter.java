package com.ravenherz.cse.util.imaging;

import openize.heic.decoder.HeicImage;
import openize.heic.decoder.PixelFormat;
import openize.io.IOFileStream;
import openize.io.IOMode;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Decodes HEIC/HEIF uploads and re-encodes them as JPEG so stored resources
 * remain browser-friendly.
 */
public final class HeicJpegConverter {

    private HeicJpegConverter() {
    }

    public static boolean isHeicExtension(String extension) {
        if (extension == null) {
            return false;
        }
        String ext = extension.toLowerCase();
        return "heic".equals(ext) || "heif".equals(ext);
    }

    public static JpegImages.Encoded toJpeg(byte[] heicBytes) throws IOException {
        return toJpeg(heicBytes, 0.95f);
    }

    public static JpegImages.Encoded toJpeg(byte[] heicBytes, float quality) throws IOException {
        if (heicBytes == null || heicBytes.length == 0) {
            throw new IOException("HEIC file is empty");
        }

        Path tmp = Files.createTempFile("cse-heic-", ".heic");
        int width;
        int height;
        int[] pixels;
        try {
            Files.write(tmp, heicBytes);
            try (IOFileStream stream = new IOFileStream(tmp.toFile(), IOMode.READ)) {
                HeicImage image = HeicImage.load(stream);
                width = Math.toIntExact(image.getWidth());
                height = Math.toIntExact(image.getHeight());
                pixels = image.getInt32Array(PixelFormat.Argb32);
            }
        } catch (RuntimeException e) {
            throw new IOException("Could not decode HEIC image", e);
        } finally {
            Files.deleteIfExists(tmp);
        }

        if (pixels == null || width <= 0 || height <= 0) {
            throw new IOException("HEIC image has no pixel data");
        }

        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        rgb.setRGB(0, 0, width, height, pixels, 0, width);
        return new JpegImages.Encoded(JpegImages.encode(rgb, quality), width, height);
    }
}
