package com.ravenherz.cse.util;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class PasswordHashes {

    private static final int MAX_PASSWORD_CHARS = 256;
    private static final PasswordEncoder ARGON2 =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public String hash(String rawPassword) {
        return ARGON2.encode(rawPassword);
    }

    public boolean verify(String rawPassword, String stored) {
        if (rawPassword == null || stored == null || stored.isBlank()) {
            return false;
        }
        if (isArgon2(stored)) {
            return ARGON2.matches(rawPassword, stored);
        }
        if (isLegacySha512(stored)) {
            byte[] expected = stored.toLowerCase().getBytes(StandardCharsets.UTF_8);
            byte[] actual = sha512Hex(rawPassword).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        }
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                rawPassword.getBytes(StandardCharsets.UTF_8));
    }

    public boolean needsRehash(String stored) {
        return stored == null || !isArgon2(stored);
    }

    public boolean isTooLong(String rawPassword) {
        return rawPassword != null && rawPassword.length() > MAX_PASSWORD_CHARS;
    }

    public static boolean isArgon2(String stored) {
        return stored != null && stored.startsWith("$argon2");
    }

    public static boolean isLegacySha512(String stored) {
        return stored != null && stored.matches("[0-9a-fA-F]{128}");
    }

    /**
     * Old client posted SHA-512 hex and Mongo still has that hex. Verify by equality
     * only — do not Argon2-hash the hex (there is no plaintext to migrate).
     */
    public static boolean isUnmigratedClientHash(String postedPassword, String stored) {
        return isLegacySha512(stored)
                && isLegacySha512(postedPassword)
                && stored.equalsIgnoreCase(postedPassword);
    }

    public static String sha512Hex(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 not available", e);
        }
    }
}
