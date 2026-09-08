package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class Mp3WaveformTest {

    @Test
    void downsampleMapsPeakWithSignIntoSignedByte() {
        short[] mono = new short[8];
        mono[0] = 32767;
        mono[1] = 100;
        mono[7] = -32768;
        byte[] peaks = Mp3Waveform.downsample(mono, 2);
        assertEquals(2, peaks.length);
        assertEquals(127, peaks[0]);
        assertEquals(-128, peaks[1]);
    }

    @Test
    void storedResolutionIs1024Times16() {
        assertEquals(16384, Mp3Waveform.COLUMNS);
    }

    @Test
    void downsampleRejectsEmptyInput() {
        assertNull(Mp3Waveform.downsample(null, Mp3Waveform.COLUMNS));
        assertNull(Mp3Waveform.downsample(new short[0], Mp3Waveform.COLUMNS));
        assertNull(Mp3Waveform.downsample(new short[] { 1 }, 0));
    }

    @Test
    void extractRejectsEmptyMp3() {
        assertNull(Mp3Waveform.extract(null));
        assertNull(Mp3Waveform.extract(new byte[0]));
        assertNull(Mp3Waveform.extract(new byte[] { 1, 2, 3, 4 }));
    }
}
