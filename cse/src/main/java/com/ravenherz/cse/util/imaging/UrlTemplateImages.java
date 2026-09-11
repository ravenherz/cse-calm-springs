package com.ravenherz.cse.util.imaging;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

import javax.imageio.ImageIO;

public final class UrlTemplateImages {

    public static final int MAX_SIDE = 256;
    static final float QUALITY = 0.85f;
    private static final String JPEG_PREFIX = "data:image/jpeg;base64,";
    private static final String PNG_PREFIX = "data:image/png;base64,";

    private UrlTemplateImages() {
    }

    public static String encode(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Image is empty");
        }
        BufferedImage src = JpegImages.read(bytes);
        if (src == null) {
            throw new IOException("Could not read image");
        }
        if (isPng(bytes) || hasAlpha(src)) {
            return PNG_PREFIX + Base64.getEncoder().encodeToString(fitPng(src, bytes));
        }
        JpegImages.Encoded encoded = JpegImages.fit(bytes, MAX_SIDE, QUALITY);
        return JPEG_PREFIX + Base64.getEncoder().encodeToString(encoded.bytes());
    }

    public static String keepOrEncode(String existing, byte[] uploaded) throws IOException {
        if (uploaded != null && uploaded.length > 0) {
            return encode(uploaded);
        }
        if (existing == null || existing.isBlank()) {
            return null;
        }
        return existing.trim();
    }

    public static byte[] rawBytes(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        String value = stored.trim();
        String payload = value;
        if (value.toLowerCase(Locale.ROOT).startsWith("data:") && value.indexOf(',') >= 0) {
            payload = value.substring(value.indexOf(',') + 1);
        }
        try {
            return Base64.getDecoder().decode(payload.replaceAll("\\s+", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] fitPng(BufferedImage src, byte[] original) throws IOException {
        int width = src.getWidth();
        int height = src.getHeight();
        int longest = Math.max(width, height);
        if (longest <= MAX_SIDE && isPng(original)) {
            return original;
        }
        int newWidth = width;
        int newHeight = height;
        if (longest > MAX_SIDE) {
            double scale = MAX_SIDE / (double) longest;
            newWidth = Math.max(1, (int) Math.round(width * scale));
            newHeight = Math.max(1, (int) Math.round(height * scale));
        }
        BufferedImage dest = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = dest.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics.drawImage(src, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return writePng(dest);
    }

    private static byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, "png", out)) {
            throw new IOException("No PNG writer available");
        }
        byte[] png = out.toByteArray();
        if (png.length == 0) {
            throw new IOException("PNG conversion produced an empty file");
        }
        return png;
    }

    private static boolean hasAlpha(BufferedImage image) {
        return image != null && image.getColorModel() != null && image.getColorModel().hasAlpha();
    }

    static boolean isPng(byte[] bytes) {
        return bytes != null && bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A;
    }
}
