package com.ravenherz.cse.util;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

public final class Mp3Waveform {

    public static final int COLUMNS = 1024 * 16;

    private static final Logger LOGGER = LoggerFactory.getLogger(Mp3Waveform.class);

    private Mp3Waveform() {
    }

    public static byte[] extract(byte[] mp3) {
        if (mp3 == null || mp3.length == 0) {
            return null;
        }
        try {
            short[] mono = decodeMono(mp3);
            return downsample(mono, COLUMNS);
        } catch (Exception e) {
            LOGGER.warn("Could not extract MP3 waveform: {}", e.getMessage());
            return null;
        }
    }

    static byte[] downsample(short[] mono, int columns) {
        if (mono == null || mono.length == 0 || columns <= 0) {
            return null;
        }
        int[] peaks = new int[columns];
        for (int i = 0; i < mono.length; i++) {
            int column = (int) ((long) i * columns / mono.length);
            if (column >= columns) {
                column = columns - 1;
            }
            short sample = mono[i];
            if (Math.abs(sample) > Math.abs(peaks[column])
                    || (Math.abs(sample) == Math.abs(peaks[column]) && sample < peaks[column])) {
                peaks[column] = sample;
            }
        }
        return toSignedBytes(peaks);
    }

    private static short[] decodeMono(byte[] mp3) throws Exception {
        Bitstream bitstream = new Bitstream(new ByteArrayInputStream(mp3));
        Decoder decoder = new Decoder();
        short[] samples = new short[16384];
        int count = 0;
        try {
            Header header;
            while ((header = bitstream.readFrame()) != null) {
                SampleBuffer buffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                int channels = Math.max(1, buffer.getChannelCount());
                short[] pcm = buffer.getBuffer();
                int length = buffer.getBufferLength();
                for (int i = 0; i + channels - 1 < length && i + channels - 1 < pcm.length; i += channels) {
                    int mixed = pcm[i];
                    if (channels > 1) {
                        mixed = (pcm[i] + pcm[i + 1]) / 2;
                    }
                    if (count == samples.length) {
                        samples = Arrays.copyOf(samples, samples.length * 2);
                    }
                    samples[count++] = (short) mixed;
                }
                bitstream.closeFrame();
            }
        } finally {
            bitstream.close();
        }
        if (count == 0) {
            return null;
        }
        return Arrays.copyOf(samples, count);
    }

    private static byte[] toSignedBytes(int[] peaks) {
        byte[] out = new byte[peaks.length];
        for (int i = 0; i < peaks.length; i++) {
            int sample = peaks[i];
            if (sample > 32767) {
                sample = 32767;
            } else if (sample < -32768) {
                sample = -32768;
            }
            out[i] = (byte) (sample >> 8);
        }
        return out;
    }
}
