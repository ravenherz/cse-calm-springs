package com.ravenherz.cse.util.imaging;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Rewrites HEIF-stored HEVC into an Annex-B elementary stream.
 * <p>
 * HEIF keeps each NAL unit behind a length prefix and hides the parameter sets in the
 * {@code hvcC} record. A raw HEVC decoder wants start codes and the parameter sets inline.
 */
final class HevcAnnexB {

    private static final byte[] START_CODE = {0x00, 0x00, 0x00, 0x01};
    private static final int LENGTH_SIZE_OFFSET = 21;
    private static final int ARRAY_COUNT_OFFSET = 22;

    private HevcAnnexB() {
    }

    /** Bytes the item payload uses for each NAL length prefix. */
    static int lengthSize(byte[] decoderConfig) throws IOException {
        if (decoderConfig == null || decoderConfig.length <= ARRAY_COUNT_OFFSET) {
            throw new IOException("HEVC decoder config is truncated");
        }
        return (decoderConfig[LENGTH_SIZE_OFFSET] & 0x03) + 1;
    }

    /** Writes the VPS, SPS and PPS held in {@code hvcC}. */
    static void writeParameterSets(byte[] decoderConfig, OutputStream out) throws IOException {
        int arrays = decoderConfig[ARRAY_COUNT_OFFSET] & 0xFF;
        int at = ARRAY_COUNT_OFFSET + 1;
        for (int i = 0; i < arrays; i++) {
            if (at + 3 > decoderConfig.length) {
                throw new IOException("HEVC decoder config is truncated");
            }
            at++;
            int count = readShort(decoderConfig, at);
            at += 2;
            for (int j = 0; j < count; j++) {
                if (at + 2 > decoderConfig.length) {
                    throw new IOException("HEVC decoder config is truncated");
                }
                int length = readShort(decoderConfig, at);
                at += 2;
                if (at + length > decoderConfig.length) {
                    throw new IOException("HEVC decoder config is truncated");
                }
                out.write(START_CODE);
                out.write(decoderConfig, at, length);
                at += length;
            }
        }
    }

    /** Writes one coded image, converting its length prefixes to start codes. */
    static void writeNalUnits(byte[] payload, int lengthSize, OutputStream out) throws IOException {
        int at = 0;
        while (at + lengthSize <= payload.length) {
            int length = 0;
            for (int i = 0; i < lengthSize; i++) {
                length = (length << 8) | (payload[at + i] & 0xFF);
            }
            at += lengthSize;
            if (length <= 0 || at + length > payload.length) {
                break;
            }
            out.write(START_CODE);
            out.write(payload, at, length);
            at += length;
        }
    }

    private static int readShort(byte[] source, int at) {
        return ((source[at] & 0xFF) << 8) | (source[at + 1] & 0xFF);
    }
}
