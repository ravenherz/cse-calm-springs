package com.ravenherz.cse.util.imaging;

import openize.heic.decoder.AuxiliaryReferenceType;
import openize.heic.decoder.HeicImage;
import openize.heic.decoder.HeicImageFrame;
import openize.heic.decoder.ImageFrameType;
import openize.heic.decoder.PixelFormat;
import openize.io.IOFileStream;
import openize.io.IOMode;
import openize.isobmff.BoxType;
import openize.isobmff.ItemReferenceBox;
import openize.isobmff.SingleItemTypeReferenceBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Decodes HEIC/HEIF uploads and re-encodes them as JPEG so stored resources
 * remain browser-friendly.
 * <p>
 * Openize materializes pixels as {@code byte[w][h][4]} and copies that graph
 * again when the file has an {@code irot}/{@code imir} transform (typical for
 * iPhone photos). A 12MP frame can need several hundred MB. This converter
 * strips those transforms before decode, applies them on a packed bitmap,
 * and stitches tiled {@code grid} primaries one cell at a time so a 12MP
 * iPhone photo does not allocate Openize's {@code byte[w][h][4]} graph.
 * The embedded thumbnail is used only when the primary is a single huge
 * frame or stitching fails.
 */
public final class HeicJpegConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeicJpegConverter.class);
    private static final Object DECODE_LOCK = new Object();
    private static final long BYTES_PER_PIXEL_BUDGET = 72L;

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
        return toJpeg(heicBytes, quality, null, null);
    }

    public static JpegImages.Encoded toJpeg(byte[] heicBytes, float quality, String uploader, String publicPath)
            throws IOException {
        if (heicBytes == null || heicBytes.length == 0) {
            throw new IOException("HEIC file is empty");
        }
        DecodeLog log = new DecodeLog(uploader, publicPath);
        synchronized (DECODE_LOCK) {
            Path tmp = Files.createTempFile("cse-heic-", ".heic");
            try {
                Files.write(tmp, heicBytes);
                try {
                    return decodeToJpeg(tmp, quality, maxDecodablePixels(), log);
                } catch (OutOfMemoryError e) {
                    LOGGER.warn("HEIC primary frame ran out of memory, trying thumbnail");
                    return decodeToJpeg(tmp, quality, 1_200_000L, log);
                }
            } catch (OutOfMemoryError e) {
                throw new IOException("Could not decode HEIC image (not enough memory)", e);
            } catch (RuntimeException e) {
                throw new IOException("Could not decode HEIC image", e);
            } finally {
                Files.deleteIfExists(tmp);
            }
        }
    }

    static long maxDecodablePixels() {
        long maxHeap = Runtime.getRuntime().maxMemory();
        if (maxHeap <= 0) {
            return 2_500_000L;
        }
        return Math.max(800_000L, Math.min(12_000_000L, maxHeap / BYTES_PER_PIXEL_BUDGET));
    }

    static BufferedImage applyHeifOrientation(BufferedImage src, int angle, int mirror) {
        if (src == null) {
            return null;
        }
        if ((angle & 0xFF) == 0 && (mirror & 0xFF) == 0) {
            return src;
        }
        AffineTransform transform = new AffineTransform();
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int destW = srcW;
        int destH = srcH;
        switch (angle & 0xFF) {
            case 1 -> {
                destW = srcH;
                destH = srcW;
                transform.translate(0, destH);
                transform.rotate(-Math.PI / 2);
            }
            case 2 -> {
                transform.translate(destW, destH);
                transform.rotate(Math.PI);
            }
            case 3 -> {
                destW = srcH;
                destH = srcW;
                transform.translate(destW, 0);
                transform.rotate(Math.PI / 2);
            }
            default -> {
            }
        }
        if ((mirror & 0xFF) == 1) {
            transform.scale(1, -1);
            transform.translate(0, -srcH);
        } else if ((mirror & 0xFF) == 2) {
            transform.scale(-1, 1);
            transform.translate(-srcW, 0);
        }
        BufferedImage dest = new BufferedImage(destW, destH, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = dest.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, destW, destH);
        graphics.drawImage(src, transform, null);
        graphics.dispose();
        return dest;
    }

    private static JpegImages.Encoded decodeToJpeg(Path heicFile, float quality, long maxPixels, DecodeLog log)
            throws IOException {
        try (IOFileStream stream = new IOFileStream(heicFile.toFile(), IOMode.READ)) {
            HeicImage image = HeicImage.load(stream);
            HeicImageFrame frame = chooseFrame(image, maxPixels);
            if (frame == null || !frame.isImage()) {
                throw new IOException("HEIC image has no pixel data");
            }
            try {
                return encodeFrame(image, frame, quality, log);
            } catch (OutOfMemoryError | IOException e) {
                HeicImageFrame thumbnail = findThumbnail(image, Math.min(maxPixels, 1_200_000L));
                if (thumbnail == null || thumbnail.getID() == frame.getID()) {
                    throw e;
                }
                LOGGER.warn("HEIC full frame failed ({}), using thumbnail {}x{}",
                        e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                        thumbnail.getWidth(),
                        thumbnail.getHeight());
                return encodeFrame(image, thumbnail, quality, log);
            }
        }
    }

    private static JpegImages.Encoded encodeFrame(HeicImage image, HeicImageFrame frame, float quality, DecodeLog log)
            throws IOException {
        Orientation orientation = stealOrientation(frame);
        BufferedImage rgb = isGrid(frame) ? stitchGrid(image, frame, log) : rasterToImage(frame);
        rgb = applyHeifOrientation(rgb, orientation.angle, orientation.mirror);
        return new JpegImages.Encoded(JpegImages.encode(rgb, quality), rgb.getWidth(), rgb.getHeight());
    }

    private static boolean isGrid(HeicImageFrame frame) {
        return frame != null && frame.getImageType() == ImageFrameType.grid;
    }

    private static BufferedImage rasterToImage(HeicImageFrame frame) throws IOException {
        int[] pixels = frame.getInt32Array(PixelFormat.Argb32);
        int width = Math.toIntExact(frame.getWidth());
        int height = Math.toIntExact(frame.getHeight());
        if (pixels == null || width <= 0 || height <= 0 || pixels.length < width * height) {
            throw new IOException("HEIC image has no pixel data");
        }
        BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        rgb.setRGB(0, 0, width, height, pixels, 0, width);
        releaseDecodedPixels(frame);
        return rgb;
    }

    private static BufferedImage stitchGrid(HeicImage image, HeicImageFrame grid, DecodeLog log) throws IOException {
        long[] tileIds = derivedIds(image, grid.getID());
        if (tileIds.length == 0) {
            throw new IOException("HEIC grid has no tiles");
        }
        Map<Long, HeicImageFrame> frames = image.getAllFrames();
        HeicImageFrame first = frames.get(tileIds[0]);
        if (first == null || !first.isImage()) {
            throw new IOException("HEIC grid tile is missing");
        }
        stealOrientation(first);
        int cellW = Math.toIntExact(first.getWidth());
        int cellH = Math.toIntExact(first.getHeight());
        int outW = Math.toIntExact(grid.getWidth());
        int outH = Math.toIntExact(grid.getHeight());
        if (cellW <= 0 || cellH <= 0 || outW <= 0 || outH <= 0) {
            throw new IOException("HEIC grid has no pixel data");
        }
        int columns = (outW + cellW - 1) / cellW;
        int rows = (outH + cellH - 1) / cellH;
        int needed = columns * rows;
        if (tileIds.length < needed) {
            throw new IOException("HEIC grid tile count does not match image size");
        }
        LOGGER.info("HEIC stitching grid {}x{} from {} tiles ({}x{}) uploader={} path={}",
                outW, outH, needed, columns, rows, log.uploader(), log.publicPath());
        BufferedImage dest = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                HeicImageFrame tile = frames.get(tileIds[row * columns + col]);
                if (tile == null || !tile.isImage()) {
                    throw new IOException("HEIC grid tile is missing");
                }
                stealOrientation(tile);
                int[] pixels = tile.getInt32Array(PixelFormat.Argb32);
                int tileW = Math.toIntExact(tile.getWidth());
                int tileH = Math.toIntExact(tile.getHeight());
                if (pixels == null || tileW <= 0 || tileH <= 0 || pixels.length < tileW * tileH) {
                    throw new IOException("HEIC grid tile has no pixel data");
                }
                blitTile(dest, col * cellW, row * cellH, pixels, tileW, tileH);
                releaseDecodedPixels(tile);
            }
        }
        return dest;
    }

    private static void releaseDecodedPixels(HeicImageFrame frame) {
        writeField(frame, "rawPixels", null);
        writeField(frame, "rawPixelsHighColorRange", null);
        writeField(frame, "cashed", Boolean.FALSE);
    }

    private static void blitTile(BufferedImage dest, int x, int y, int[] src, int tileW, int tileH) {
        int copyW = Math.min(tileW, dest.getWidth() - x);
        int copyH = Math.min(tileH, dest.getHeight() - y);
        if (copyW <= 0 || copyH <= 0) {
            return;
        }
        if (copyW == tileW && copyH == tileH) {
            dest.setRGB(x, y, tileW, tileH, src, 0, tileW);
            return;
        }
        int[] slice = new int[copyW * copyH];
        for (int row = 0; row < copyH; row++) {
            System.arraycopy(src, row * tileW, slice, row * copyW, copyW);
        }
        dest.setRGB(x, y, copyW, copyH, slice, 0, copyW);
    }

    private static long[] derivedIds(HeicImage image, long frameId) {
        ItemReferenceBox iref = image.getHeader().getMeta().getiref();
        if (iref == null || iref.references == null) {
            return new long[0];
        }
        long[] any = null;
        for (Object raw : iref.references) {
            if (!(raw instanceof SingleItemTypeReferenceBox ref)
                    || ref.from_item_ID != frameId
                    || ref.to_item_ID == null
                    || ref.to_item_ID.length == 0) {
                continue;
            }
            if (ref.type == BoxType.dimg) {
                return ref.to_item_ID;
            }
            if (any == null) {
                any = ref.to_item_ID;
            }
        }
        return any != null ? any : new long[0];
    }

    private static HeicImageFrame chooseFrame(HeicImage image, long maxPixels) {
        HeicImageFrame primary = image.getDefaultFrame();
        if (primary != null && primary.isImage() && (isGrid(primary) || pixelsOf(primary) <= maxPixels)) {
            return primary;
        }
        HeicImageFrame thumbnail = findThumbnail(image, maxPixels);
        if (thumbnail != null) {
            LOGGER.info("HEIC primary {}x{} exceeds decode budget {} px; using thumbnail {}x{}",
                    primary == null ? 0 : primary.getWidth(),
                    primary == null ? 0 : primary.getHeight(),
                    maxPixels,
                    thumbnail.getWidth(),
                    thumbnail.getHeight());
            return thumbnail;
        }
        return primary;
    }

    private static HeicImageFrame findThumbnail(HeicImage image, long maxPixels) {
        HeicImageFrame best = null;
        long bestPixels = 0;
        Map<Long, HeicImageFrame> frames = image.getAllFrames();
        if (frames == null) {
            return null;
        }
        for (HeicImageFrame frame : frames.values()) {
            if (frame == null || !frame.isImage()) {
                continue;
            }
            if (frame.getDerivativeType() != BoxType.thmb) {
                continue;
            }
            if (isAuxiliary(frame)) {
                continue;
            }
            long pixels = pixelsOf(frame);
            if (pixels <= 0 || pixels > maxPixels) {
                continue;
            }
            if (pixels > bestPixels) {
                best = frame;
                bestPixels = pixels;
            }
        }
        return best;
    }

    private static boolean isAuxiliary(HeicImageFrame frame) {
        AuxiliaryReferenceType type = frame.getAuxiliaryReferenceType();
        return type != null && type != AuxiliaryReferenceType.Undefined;
    }

    private static long pixelsOf(HeicImageFrame frame) {
        return Math.max(0L, frame.getWidth()) * Math.max(0L, frame.getHeight());
    }

    private static Orientation stealOrientation(HeicImageFrame frame) {
        int angle = readByte(frame, "imageRotationAngle");
        int mirror = readByte(frame, "imageMirrorAxis");
        writeByte(frame, "imageRotationAngle", (byte) 0);
        writeByte(frame, "imageMirrorAxis", (byte) 0);
        writeField(frame, "alphaReference", null);
        return new Orientation(angle, mirror);
    }

    private static int readByte(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getByte(target) & 0xFF;
        } catch (ReflectiveOperationException e) {
            return 0;
        }
    }

    private static void writeByte(Object target, String name, byte value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.setByte(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void writeField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private record Orientation(int angle, int mirror) {
    }

    private record DecodeLog(String uploader, String publicPath) {
        DecodeLog {
            uploader = blankToDash(uploader);
            publicPath = blankToDash(publicPath);
        }

        private static String blankToDash(String value) {
            return value == null || value.isBlank() ? "-" : value.trim();
        }
    }
}
