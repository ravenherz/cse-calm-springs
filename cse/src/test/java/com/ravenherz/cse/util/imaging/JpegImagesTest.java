package com.ravenherz.cse.util.imaging;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpegImagesTest {

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
    void squareThumbSubsampleStillFills64() throws IOException {
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
}
