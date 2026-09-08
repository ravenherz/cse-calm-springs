package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.DalStrings;

import java.security.SecureRandom;
import java.util.HexFormat;

public class StringUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomPath(String fileName) {
        return DalStrings.generateRandomPath(fileName);
    }

    public static String generateRandomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
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

    public static String formatByteSize(long bytes) {
        return DalStrings.formatByteSize(bytes);
    }
}
