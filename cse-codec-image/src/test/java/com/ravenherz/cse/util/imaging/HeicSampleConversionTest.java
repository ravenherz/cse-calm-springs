package com.ravenherz.cse.util.imaging;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HeicSampleConversionTest {

    @Test
    void tiledIphoneSamplesKeepFullResolution() throws Exception {
        Path ok = sample("ok.HEIC");
        Path notOk = sample("not-ok.HEIC");
        assumeTrue(Files.isRegularFile(ok), "tmp/ok.HEIC is missing");
        assumeTrue(Files.isRegularFile(notOk), "tmp/not-ok.HEIC is missing");

        JpegImages.Encoded okJpeg = HeicJpegConverter.toJpeg(Files.readAllBytes(ok), 0.95f);
        JpegImages.Encoded notOkJpeg = HeicJpegConverter.toJpeg(Files.readAllBytes(notOk), 0.95f);

        assertTrue(Math.max(okJpeg.width(), okJpeg.height()) >= 2200,
                "ok.HEIC should stay full-res, got " + size(okJpeg));
        assertTrue(okJpeg.bytes().length > 200_000,
                "ok.HEIC JPEG should be a full photo, got " + okJpeg.bytes().length + " bytes");

        assertTrue(Math.max(notOkJpeg.width(), notOkJpeg.height()) >= 3000,
                "not-ok.HEIC should stay full-res, not the 240x320 preview, got " + size(notOkJpeg));
        assertTrue(notOkJpeg.bytes().length > 200_000,
                "not-ok.HEIC JPEG should be a full photo, got " + notOkJpeg.bytes().length + " bytes");
    }

    private static String size(JpegImages.Encoded jpeg) {
        return jpeg.width() + "x" + jpeg.height();
    }

    private static Path sample(String name) {
        Path fromCse = Path.of("..", "tmp", name).toAbsolutePath().normalize();
        if (Files.isRegularFile(fromCse)) {
            return fromCse;
        }
        return Path.of("tmp", name).toAbsolutePath().normalize();
    }
}
