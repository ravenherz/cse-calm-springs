package com.ravenherz.rhzwe.util;

public class StringUtils {

    private static final String PATH_ALPHABET = "abcde/fgh/ijkl/mnopq//rstu/vwzyz0/1234/567890";
    private static final String TOKEN_ALPHABET = "ABCDEFJHIJKLMNOPQRSTUVWXYZ"
            + "abcdefghijklmnopqrstuvwxyz"
            + "1234567890";

    public static String generateRandomPath(String fileName) {
        boolean badPath = true;
        String path = "";
        while (badPath) {
            path = generateRandomString(PATH_ALPHABET, 48, false);
            badPath = isBadPath(path);
        }
        return String.format("/%s/%s", path, fileName);
    }

    private static boolean isBadPath(String path) {
        if (path.startsWith("/") || path.endsWith("/")) {
            return true;
        }
        String[] pathParts = path.split("/");
        return pathParts.length < 3 || pathParts[pathParts.length - 1].length() < 6;
    }

    public static String generateRandomToken() {
        return generateRandomString(
                TOKEN_ALPHABET, 256);
    }

    public static String generateRandomString(String alphabetSource, int iterations) {
        return generateRandomString(alphabetSource, iterations, true);
    }

    public static String generateRandomString(String alphabetSource, int iterations,
            boolean doublesAreLegal) {
        StringBuilder out = new StringBuilder();
        char previousChar = 0;
        char[] alphabet = alphabetSource.toCharArray();
        for (int i = 0; i < iterations; i++) {

            int random = (int) Math.floor(Math.random() * (alphabet.length));
            char append = alphabet[Math.min(random, alphabet.length)];
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
