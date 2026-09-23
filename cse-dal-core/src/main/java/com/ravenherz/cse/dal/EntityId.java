package com.ravenherz.cse.dal;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.Objects;

/**
 * Document id stored as 24 hex characters. Mongo persists this as an ObjectId.
 */
public final class EntityId implements Serializable {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String hex;

    private EntityId(String hex) {
        this.hex = hex;
    }

    public static EntityId generate() {
        byte[] bytes = new byte[12];
        long now = System.currentTimeMillis() / 1000L;
        bytes[0] = (byte) (now >> 24);
        bytes[1] = (byte) (now >> 16);
        bytes[2] = (byte) (now >> 8);
        bytes[3] = (byte) now;
        byte[] rest = new byte[8];
        RANDOM.nextBytes(rest);
        System.arraycopy(rest, 0, bytes, 4, 8);
        return new EntityId(toHex(bytes));
    }

    public static EntityId of(String hex) {
        if (hex == null) {
            return null;
        }
        String trimmed = hex.trim();
        if (trimmed.length() != 24 || !isHex(trimmed)) {
            throw new IllegalArgumentException("Entity id must be 24 hex characters");
        }
        return new EntityId(trimmed.toLowerCase());
    }

    public static boolean isHex(String value) {
        if (value == null || value.length() != 24) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean digit = c >= '0' && c <= '9';
            boolean lower = c >= 'a' && c <= 'f';
            boolean upper = c >= 'A' && c <= 'F';
            if (!digit && !lower && !upper) {
                return false;
            }
        }
        return true;
    }

    public String hex() {
        return hex;
    }

    public String toHexString() {
        return hex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntityId entityId)) {
            return false;
        }
        return hex.equals(entityId.hex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hex);
    }

    @Override
    public String toString() {
        return hex;
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        char[] digits = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xff;
            out[i * 2] = digits[v >>> 4];
            out[i * 2 + 1] = digits[v & 0x0f];
        }
        return new String(out);
    }
}
