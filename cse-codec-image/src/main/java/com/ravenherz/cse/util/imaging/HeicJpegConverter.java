package com.ravenherz.cse.util.imaging;

import com.ravenherz.cse.util.video.FfmpegBinaries;
import com.ravenherz.cse.util.video.FfmpegProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Decodes HEIC/HEIF uploads and re-encodes them as JPEG so stored resources
 * remain browser-friendly.
 * <p>
 * A HEIC photo is HEVC intra frames in an ISOBMFF container, and an iPhone writes the
 * primary image as a grid of tiles. We read the container here, hand the tiles to the
 * bundled FFmpeg as one elementary stream, and let it decode, assemble and rotate them.
 * Decoding in a real HEVC decoder is what keeps the in-loop deblocking filter, without
 * which tile edges stay visible as a grid over the photo.
 */
public final class HeicJpegConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeicJpegConverter.class);
    private static final Duration DECODE_TIMEOUT = Duration.ofMinutes(10);
    private static final long MAX_PIXELS = Integer.MAX_VALUE / 3L;

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
        HeifFile heif = HeifFile.parse(heicBytes);
        int primary = heif.primaryItemId();
        Path work = Files.createTempDirectory("cse-heic-");
        try {
            Path elementary = work.resolve("image.hevc");
            Layout layout = writeElementaryStream(heif, primary, elementary);
            List<HeifFile.Transform> transforms = heif.transforms(primary);
            int[] size = displaySize(layout, transforms);
            if ((long) size[0] * size[1] > MAX_PIXELS) {
                throw new IOException("HEIC image is too large to decode: " + size[0] + "x" + size[1]);
            }
            LOGGER.info("HEIC decoding {}x{} from {} tile(s) uploader={} path={}",
                    size[0], size[1], layout.columns() * layout.rows(),
                    blankToDash(uploader), blankToDash(publicPath));
            Path pixels = work.resolve("image.bgr");
            decode(elementary, pixels, filterChain(layout, transforms));
            BufferedImage image = readPixels(pixels, size[0], size[1]);
            return new JpegImages.Encoded(JpegImages.encode(image, quality), size[0], size[1]);
        } catch (OutOfMemoryError e) {
            throw new IOException("Could not decode HEIC image (not enough memory)", e);
        } finally {
            deleteQuietly(work);
        }
    }

    /** Tile geometry of the primary image, before rotation. */
    record Layout(int columns, int rows, int width, int height) {
    }

    private static Layout writeElementaryStream(HeifFile heif, int primary, Path dest) throws IOException {
        String type = heif.itemType(primary);
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(dest), 1 << 16)) {
            if ("grid".equals(type)) {
                return writeGrid(heif, primary, out);
            }
            if ("hvc1".equals(type) || "hev1".equals(type)) {
                return writeSingle(heif, primary, out);
            }
            throw new IOException("Unsupported HEIC image type: " + type);
        }
    }

    private static Layout writeGrid(HeifFile heif, int primary, OutputStream out) throws IOException {
        HeifFile.Grid grid = heif.grid(primary);
        List<Integer> tiles = heif.derivedFrom(primary);
        int needed = grid.columns() * grid.rows();
        if (tiles.size() < needed) {
            throw new IOException("HEIC grid needs " + needed + " tiles but lists " + tiles.size());
        }
        byte[] config = heif.decoderConfig(tiles.get(0));
        int lengthSize = HevcAnnexB.lengthSize(config);
        for (int i = 0; i < needed; i++) {
            HevcAnnexB.writeParameterSets(config, out);
            HevcAnnexB.writeNalUnits(heif.itemData(tiles.get(i)), lengthSize, out);
        }
        return new Layout(grid.columns(), grid.rows(), grid.width(), grid.height());
    }

    private static Layout writeSingle(HeifFile heif, int primary, OutputStream out) throws IOException {
        byte[] config = heif.decoderConfig(primary);
        HevcAnnexB.writeParameterSets(config, out);
        HevcAnnexB.writeNalUnits(heif.itemData(primary), HevcAnnexB.lengthSize(config), out);
        int[] size = heif.size(primary);
        if (size == null || size[0] <= 0 || size[1] <= 0) {
            throw new IOException("HEIC image has no size");
        }
        return new Layout(1, 1, size[0], size[1]);
    }

    /**
     * Assembles the tiles, trims the padding the grid adds, then applies the container's
     * rotation and mirroring in the order the file lists them.
     */
    static String filterChain(Layout layout, List<HeifFile.Transform> transforms) {
        List<String> steps = new ArrayList<>();
        if (layout.columns() * layout.rows() > 1) {
            steps.add("tile=" + layout.columns() + "x" + layout.rows());
        }
        steps.add("crop=" + layout.width() + ":" + layout.height() + ":0:0");
        for (HeifFile.Transform transform : transforms) {
            if ("irot".equals(transform.type())) {
                switch (transform.value()) {
                    case 1 -> steps.add("transpose=2");
                    case 2 -> {
                        steps.add("hflip");
                        steps.add("vflip");
                    }
                    case 3 -> steps.add("transpose=1");
                    default -> {
                    }
                }
            } else if ("imir".equals(transform.type())) {
                steps.add(transform.value() == 0 ? "hflip" : "vflip");
            }
        }
        return String.join(",", steps);
    }

    static int[] displaySize(Layout layout, List<HeifFile.Transform> transforms) {
        int width = layout.width();
        int height = layout.height();
        for (HeifFile.Transform transform : transforms) {
            if ("irot".equals(transform.type()) && (transform.value() == 1 || transform.value() == 3)) {
                int swap = width;
                width = height;
                height = swap;
            }
        }
        return new int[] {width, height};
    }

    private static void decode(Path elementary, Path pixels, String filter) throws IOException {
        FfmpegProcess.run(List.of(
                FfmpegBinaries.ffmpeg(),
                "-hide_banner",
                "-y",
                "-nostdin",
                "-hwaccel", "none",
                "-f", "hevc",
                "-i", elementary.toAbsolutePath().toString(),
                "-frames:v", "1",
                "-vf", filter,
                "-f", "rawvideo",
                "-pix_fmt", "bgr24",
                pixels.toAbsolutePath().toString()), DECODE_TIMEOUT);
    }

    /** Wraps the raw frame without copying it; {@code bgr24} is already the layout of TYPE_3BYTE_BGR. */
    private static BufferedImage readPixels(Path pixels, int width, int height) throws IOException {
        long expected = (long) width * height * 3;
        if (!Files.isRegularFile(pixels) || Files.size(pixels) != expected) {
            throw new IOException("FFmpeg produced " + (Files.exists(pixels) ? Files.size(pixels) : 0)
                    + " bytes for a " + width + "x" + height + " frame, expected " + expected);
        }
        byte[] bytes = Files.readAllBytes(pixels);
        DataBufferByte buffer = new DataBufferByte(bytes, bytes.length);
        WritableRaster raster = Raster.createInterleavedRaster(
                buffer, width, height, width * 3, 3, new int[] {2, 1, 0}, null);
        ColorModel model = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        return new BufferedImage(model, raster, false, null);
    }

    private static void deleteQuietly(Path directory) {
        try (Stream<Path> tree = Files.walk(directory)) {
            tree.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
