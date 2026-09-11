package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountAvatarsTest {

    @Test
    void storeSquareCropsToJpegDataUrl() throws IOException {
        String stored = AccountAvatars.store(jpegDataUrl(40, 20));
        assertNotNull(stored);
        assertTrue(stored.startsWith(AccountAvatars.JPEG_PREFIX), stored);
        byte[] jpeg = Base64.getDecoder().decode(stored.substring(AccountAvatars.JPEG_PREFIX.length()));
        BufferedImage out = ImageIO.read(new ByteArrayInputStream(jpeg));
        assertNotNull(out);
        assertEquals(AccountAvatars.SIZE_PX, out.getWidth());
        assertEquals(AccountAvatars.SIZE_PX, out.getHeight());
    }

    @Test
    void storeRejectsOversizedPayload() {
        String huge = "data:image/jpeg;base64," + "A".repeat(AccountAvatars.MAX_INPUT_CHARS);
        IOException error = assertThrows(IOException.class, () -> AccountAvatars.store(huge));
        assertEquals("Avatar is too large", error.getMessage());
    }

    @Test
    void storeRejectsNonImageDataUrl() {
        IOException error = assertThrows(IOException.class,
                () -> AccountAvatars.store("data:text/plain;base64,Zm9v"));
        assertEquals("Avatar must be an image", error.getMessage());
    }

    private static String jpegDataUrl(int width, int height) throws IOException {
        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = src.createGraphics();
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();
        byte[] jpeg = com.ravenherz.cse.util.imaging.JpegImages.encode(src, 0.9f);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpeg);
    }
}
