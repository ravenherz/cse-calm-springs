package com.ravenherz.cse.util.imaging;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

public final class JpegImages {

    private JpegImages() {
    }

    public static byte[] encode(BufferedImage image, float quality) throws IOException {
        if (image == null) {
            throw new IOException("Image is empty");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer available");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(clampQuality(quality));
        }
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(toRgb(image), null, null), param);
            ios.flush();
        } finally {
            writer.dispose();
        }
        byte[] jpeg = out.toByteArray();
        if (jpeg.length == 0) {
            throw new IOException("JPEG conversion produced an empty file");
        }
        return jpeg;
    }

    /**
     * Builds a JPEG preview scaled to {@code maxWidth} if the source is wider.
     * Returns empty when the source is already at or below that width.
     */
    public static Optional<Encoded> scaledPreview(byte[] imageBytes, int maxWidth, float quality)
            throws IOException {
        if (imageBytes == null || imageBytes.length == 0 || maxWidth <= 0) {
            return Optional.empty();
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (src == null) {
            throw new IOException("Could not read image");
        }
        if (src.getWidth() <= maxWidth) {
            return Optional.empty();
        }
        int newWidth = maxWidth;
        int newHeight = Math.max(1, Math.round(src.getHeight() * (maxWidth / (float) src.getWidth())));
        BufferedImage dest = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = dest.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, newWidth, newHeight);
        graphics.drawImage(src, 0, 0, newWidth, newHeight, null);
        graphics.dispose();
        return Optional.of(new Encoded(encode(dest, quality), newWidth, newHeight));
    }

    /**
     * Always returns a JPEG, scaled down when wider than {@code maxWidth}.
     * Used for MP3 covers, which have no original image resource to fall back to.
     */
    /**
     * Center-crops to a square and scales to {@code size}×{@code size} JPEG.
     */
    public static Encoded squareThumb(byte[] imageBytes, int size, float quality) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Image is empty");
        }
        if (size <= 0) {
            throw new IOException("Thumb size must be positive");
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (src == null) {
            throw new IOException("Could not read image");
        }
        int side = Math.min(src.getWidth(), src.getHeight());
        if (side <= 0) {
            throw new IOException("Image has no pixels");
        }
        int x = (src.getWidth() - side) / 2;
        int y = (src.getHeight() - side) / 2;
        BufferedImage dest = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = dest.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, size, size);
        graphics.drawImage(src, 0, 0, size, size, x, y, x + side, y + side, null);
        graphics.dispose();
        return new Encoded(encode(dest, quality), size, size);
    }

    public static Encoded toJpeg(byte[] imageBytes, int maxWidth, float quality) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Image is empty");
        }
        BufferedImage src = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (src == null) {
            throw new IOException("Could not read image");
        }
        if (maxWidth > 0 && src.getWidth() > maxWidth) {
            Optional<Encoded> scaled = scaledPreview(imageBytes, maxWidth, quality);
            if (scaled.isPresent()) {
                return scaled.get();
            }
        }
        return new Encoded(encode(src, quality), src.getWidth(), src.getHeight());
    }

    public static float clampQuality(float quality) {
        if (Float.isNaN(quality) || quality <= 0f) {
            return 0.95f;
        }
        return Math.min(1f, quality);
    }

    private static BufferedImage toRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgb.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        graphics.drawImage(src, 0, 0, null);
        graphics.dispose();
        return rgb;
    }

    public static final class Encoded {
        private final byte[] bytes;
        private final int width;
        private final int height;

        public Encoded(byte[] bytes, int width, int height) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
        }

        public byte[] bytes() {
            return bytes;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }
    }
}
