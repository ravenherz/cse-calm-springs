package com.ravenherz.cse.dal;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * String helpers needed by CMS entities. Lives here so {@code cse-db-api}
 * does not depend on {@code com.ravenherz.cse.util}.
 */
public final class DalStrings {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PATH_ALPHABET = "abcde/fgh/ijkl/mnopq//rstu/vwzyz0/1234/567890";

    private DalStrings() {
    }

    public static String generateRandomPath(String fileName) {
        boolean badPath = true;
        String path = "";
        while (badPath) {
            path = generateRandomString(PATH_ALPHABET, 48, false);
            badPath = isBadPath(path);
        }
        return String.format("/%s/%s", path, fileName);
    }

    public static String formatByteSize(long bytes) {
        if (bytes > 300_000) {
            return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        }
        if (bytes > 20_000) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        }
        return bytes + " bytes";
    }

    private static boolean isBadPath(String path) {
        if (path.startsWith("/") || path.endsWith("/")) {
            return true;
        }
        String[] pathParts = path.split("/");
        return pathParts.length < 3 || pathParts[pathParts.length - 1].length() < 6;
    }

    private static String generateRandomString(String alphabetSource, int iterations,
            boolean doublesAreLegal) {
        StringBuilder out = new StringBuilder();
        char previousChar = 0;
        char[] alphabet = alphabetSource.toCharArray();
        for (int i = 0; i < iterations; i++) {
            char append = alphabet[RANDOM.nextInt(alphabet.length)];
            if (!doublesAreLegal && append == previousChar) {
                i--;
            } else {
                out.append(append);
            }
            previousChar = append;
        }
        return out.toString();
    }
}
