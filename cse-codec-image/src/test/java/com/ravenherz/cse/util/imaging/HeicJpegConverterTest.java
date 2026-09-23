package com.ravenherz.cse.util.imaging;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeicJpegConverterTest {

    @Test
    void isHeicExtensionRecognizesHeicAndHeif() {
        assertTrue(HeicJpegConverter.isHeicExtension("heic"));
        assertTrue(HeicJpegConverter.isHeicExtension("HEIF"));
        assertFalse(HeicJpegConverter.isHeicExtension("jpg"));
        assertFalse(HeicJpegConverter.isHeicExtension(null));
    }

    @Test
    void emptyBytesFail() {
        assertThrows(IOException.class, () -> HeicJpegConverter.toJpeg(new byte[0], 0.9f));
    }

    @Test
    void maxDecodablePixelsStaysWithinHeapBudget() {
        long pixels = HeicJpegConverter.maxDecodablePixels();
        assertTrue(pixels >= 800_000L, String.valueOf(pixels));
        assertTrue(pixels <= 12_000_000L, String.valueOf(pixels));
        assertTrue(pixels * 72L <= Runtime.getRuntime().maxMemory()
                || pixels == 800_000L, String.valueOf(pixels));
    }

    @Test
    void applyHeifOrientationRotates90Clockwise() {
        BufferedImage src = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 1, 1);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(1, 0, 1, 1);
        graphics.dispose();
        BufferedImage dest = HeicJpegConverter.applyHeifOrientation(src, 3, 0);
        assertEquals(1, dest.getWidth());
        assertEquals(2, dest.getHeight());
        assertEquals(Color.RED.getRGB(), dest.getRGB(0, 0) | 0xFF000000);
        assertEquals(Color.BLUE.getRGB(), dest.getRGB(0, 1) | 0xFF000000);
    }

    @Test
    void applyHeifOrientationFlipsHorizontally() {
        BufferedImage src = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 1, 1);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(1, 0, 1, 1);
        graphics.dispose();
        BufferedImage dest = HeicJpegConverter.applyHeifOrientation(src, 0, 2);
        assertEquals(2, dest.getWidth());
        assertEquals(1, dest.getHeight());
        assertEquals(Color.BLUE.getRGB(), dest.getRGB(0, 0) | 0xFF000000);
        assertEquals(Color.RED.getRGB(), dest.getRGB(1, 0) | 0xFF000000);
    }
}
