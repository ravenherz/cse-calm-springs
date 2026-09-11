package com.ravenherz.cse.util;

import com.ravenherz.cse.util.imaging.JpegImages;

import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

/**
 * Stores an account avatar as a JPEG data URL on {@code AccountData.avatar}.
 */
public final class AccountAvatars {

    public static final int SIZE_PX = 256;
    public static final int MAX_INPUT_CHARS = 400_000;
    public static final String JPEG_PREFIX = "data:image/jpeg;base64,";

    private AccountAvatars() {
    }

    public static String store(String dataUrl) throws IOException {
        if (dataUrl == null || dataUrl.isBlank()) {
            return null;
        }
        if (dataUrl.length() > MAX_INPUT_CHARS) {
            throw new IOException("Avatar is too large");
        }
        byte[] raw = decode(dataUrl);
        JpegImages.Encoded encoded = JpegImages.squareThumb(raw, SIZE_PX, 0.85f);
        return JPEG_PREFIX + Base64.getEncoder().encodeToString(encoded.bytes());
    }

    static byte[] decode(String value) throws IOException {
        String payload = value.trim();
        int comma = payload.indexOf(',');
        if (payload.regionMatches(true, 0, "data:", 0, 5)) {
            if (comma < 0) {
                throw new IOException("Could not read that image");
            }
            String meta = payload.substring(5, comma).toLowerCase(Locale.ROOT);
            if (!meta.startsWith("image/") || !meta.contains("base64")) {
                throw new IOException("Avatar must be an image");
            }
            payload = payload.substring(comma + 1);
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(payload);
            if (bytes.length == 0) {
                throw new IOException("Could not read that image");
            }
            return bytes;
        } catch (IllegalArgumentException ex) {
            throw new IOException("Could not read that image");
        }
    }
}
