package com.ravenherz.cse.util.imaging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    void gridIsAssembledThenCroppedToTheDeclaredSize() {
        HeicJpegConverter.Layout layout = new HeicJpegConverter.Layout(9, 6, 8064, 6048);
        assertEquals("tile=9x6,crop=8064:6048:0:0", HeicJpegConverter.filterChain(layout, List.of()));
    }

    @Test
    void singleTileImageSkipsTheTileFilter() {
        HeicJpegConverter.Layout layout = new HeicJpegConverter.Layout(1, 1, 1024, 768);
        assertEquals("crop=1024:768:0:0", HeicJpegConverter.filterChain(layout, List.of()));
    }

    @Test
    void rotationBecomesATransposeAndSwapsTheDisplaySize() {
        HeicJpegConverter.Layout layout = new HeicJpegConverter.Layout(9, 6, 8064, 6048);
        List<HeifFile.Transform> quarterTurn = List.of(new HeifFile.Transform("irot", 3));

        assertEquals("tile=9x6,crop=8064:6048:0:0,transpose=1",
                HeicJpegConverter.filterChain(layout, quarterTurn));
        assertArrayEquals(new int[] {6048, 8064}, HeicJpegConverter.displaySize(layout, quarterTurn));
    }

    @Test
    void halfTurnKeepsTheDisplaySize() {
        HeicJpegConverter.Layout layout = new HeicJpegConverter.Layout(1, 1, 400, 300);
        List<HeifFile.Transform> halfTurn = List.of(new HeifFile.Transform("irot", 2));

        assertEquals("crop=400:300:0:0,hflip,vflip", HeicJpegConverter.filterChain(layout, halfTurn));
        assertArrayEquals(new int[] {400, 300}, HeicJpegConverter.displaySize(layout, halfTurn));
    }

    @Test
    void mirrorFollowsTheAxisTheFileDeclares() {
        HeicJpegConverter.Layout layout = new HeicJpegConverter.Layout(1, 1, 400, 300);

        assertEquals("crop=400:300:0:0,hflip",
                HeicJpegConverter.filterChain(layout, List.of(new HeifFile.Transform("imir", 0))));
        assertEquals("crop=400:300:0:0,vflip",
                HeicJpegConverter.filterChain(layout, List.of(new HeifFile.Transform("imir", 1))));
    }
}
