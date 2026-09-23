package com.ravenherz.cse.util.imaging;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpegImagesTest {

    @Test
    void looksLikeJpegMatchesSoiMarker() {
        assertTrue(JpegImages.looksLikeJpeg(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}));
        assertFalse(JpegImages.looksLikeJpeg(new byte[] {0, 0, 0, 'f', 't', 'y', 'p'}));
        assertFalse(JpegImages.looksLikeJpeg(null));
    }

    @Test
    void squareThumbCenterCropsAndScalesToSize() throws IOException {
        BufferedImage src = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 10, 20);
        graphics.setColor(Color.GREEN);
        graphics.fillRect(10, 0, 20, 20);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(30, 0, 10, 20);
        graphics.dispose();
        byte[] jpeg = JpegImages.encode(src, 0.9f);
        JpegImages.Encoded thumb = JpegImages.squareThumb(jpeg, 64, 0.8f);
        assertEquals(64, thumb.width());
        assertEquals(64, thumb.height());
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(thumb.bytes()));
        assertNotNull(out);
        assertEquals(64, out.getWidth());
        assertEquals(64, out.getHeight());
        Color center = new Color(out.getRGB(32, 32));
        assertTrue(center.getGreen() > 150, center.toString());
        assertTrue(center.getRed() < 80, center.toString());
        assertTrue(center.getBlue() < 80, center.toString());
    }

    @Test
    void squareThumbFromLargerImageFillsSize() throws IOException {
        BufferedImage src = new BufferedImage(320, 240, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, 320, 240);
        graphics.dispose();
        JpegImages.Encoded thumb = JpegImages.squareThumb(JpegImages.encode(src, 0.9f), 64, 0.8f);
        assertEquals(64, thumb.width());
        assertEquals(64, thumb.height());
        assertTrue(thumb.bytes().length > 0);
    }

    @Test
    void squareThumbBicubicMixesNeighboringStripes() throws IOException {
        int width = 512;
        int height = 512;
        int stripe = 4;
        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        for (int x = 0; x < width; x += stripe) {
            graphics.setColor((x / stripe) % 2 == 0 ? Color.ORANGE : Color.BLUE);
            graphics.fillRect(x, 0, stripe, height);
        }
        graphics.dispose();
        JpegImages.Encoded thumb = JpegImages.squareThumb(JpegImages.encode(src, 0.95f), 64, 0.95f);
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(thumb.bytes()));
        assertNotNull(out);
        Color sample = new Color(out.getRGB(32, 32));
        assertTrue(sample.getRed() > 40, sample.toString());
        assertTrue(sample.getBlue() > 40, sample.toString());
    }

    @Test
    void encodedJpegKeepsChromaOnEveryPixel() throws IOException {
        BufferedImage src = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, 32, 32);
        graphics.dispose();
        byte[] jpeg = JpegImages.encode(src, 0.9f);
        int[] sampling = chromaSampling(jpeg);
        assertEquals(1, sampling[0]);
        assertEquals(1, sampling[1]);
        assertEquals(1, sampling[2]);
        assertEquals(1, sampling[3]);
        assertEquals(1, sampling[4]);
        assertEquals(1, sampling[5]);
    }

    private static int[] chromaSampling(byte[] jpeg) {
        for (int i = 0; i < jpeg.length - 10; i++) {
            if ((jpeg[i] & 0xFF) == 0xFF && (jpeg[i + 1] & 0xFF) == 0xC0) {
                int components = jpeg[i + 9] & 0xFF;
                int[] factors = new int[components * 2];
                int offset = i + 10;
                for (int c = 0; c < components; c++) {
                    int sampling = jpeg[offset + 1] & 0xFF;
                    factors[c * 2] = sampling >> 4;
                    factors[c * 2 + 1] = sampling & 0x0F;
                    offset += 3;
                }
                return factors;
            }
        }
        throw new AssertionError("JPEG has no baseline frame header");
    }
}
