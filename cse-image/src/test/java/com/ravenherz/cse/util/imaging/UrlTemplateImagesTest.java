package com.ravenherz.cse.util.imaging;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlTemplateImagesTest {

    @Test
    void pngKeepsTransparentPixels() throws IOException {
        byte[] png = transparentPng(32, 16);
        String stored = UrlTemplateImages.encode(png);
        assertTrue(stored.startsWith("data:image/png;base64,"), stored);
        BufferedImage out = decode(stored);
        assertEquals(32, out.getWidth());
        assertEquals(16, out.getHeight());
        assertEquals(0, alpha(out, 0, 0));
        assertEquals(255, alpha(out, 16, 8));
        Color solid = new Color(out.getRGB(16, 8), true);
        assertEquals(220, solid.getRed());
        assertEquals(40, solid.getGreen());
        assertEquals(40, solid.getBlue());
    }

    @Test
    void largePngIsScaledAndKeepsAlpha() throws IOException {
        byte[] png = transparentPng(400, 100);
        String stored = UrlTemplateImages.encode(png);
        assertTrue(stored.startsWith("data:image/png;base64,"), stored);
        BufferedImage out = decode(stored);
        assertEquals(256, out.getWidth());
        assertEquals(64, out.getHeight());
        assertEquals(0, alpha(out, 0, 0));
        assertTrue(alpha(out, 128, 32) > 200);
    }

    @Test
    void jpegStaysJpeg() throws IOException {
        BufferedImage src = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 40, 20);
        graphics.dispose();
        String stored = UrlTemplateImages.encode(JpegImages.encode(src, 0.9f));
        assertTrue(stored.startsWith("data:image/jpeg;base64,"), stored);
    }

    private static byte[] transparentPng(int width, int height) throws IOException {
        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setComposite(java.awt.AlphaComposite.Clear);
        graphics.fillRect(0, 0, width, height);
        graphics.setComposite(java.awt.AlphaComposite.Src);
        graphics.setColor(new Color(220, 40, 40, 255));
        graphics.fillRect(width / 4, height / 4, width / 2, height / 2);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(src, "png", out);
        return out.toByteArray();
    }

    private static BufferedImage decode(String dataUri) throws IOException {
        int comma = dataUri.indexOf(',');
        byte[] bytes = Base64.getDecoder().decode(dataUri.substring(comma + 1));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        assertNotNull(image);
        return image;
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) >>> 24) & 0xFF;
    }
}
