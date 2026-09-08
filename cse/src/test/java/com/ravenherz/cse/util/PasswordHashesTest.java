package com.ravenherz.cse.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashesTest {

    private final PasswordHashes hashes = new PasswordHashes();

    @Test
    void argon2idRoundTrip() {
        String stored = hashes.hash("correct horse");
        assertTrue(PasswordHashes.isArgon2(stored));
        assertTrue(hashes.verify("correct horse", stored));
        assertFalse(hashes.verify("wrong", stored));
        assertFalse(hashes.needsRehash(stored));
    }

    @Test
    void legacySha512HexVerifiesPlaintextThenNeedsRehash() {
        String stored = PasswordHashes.sha512Hex("secret");
        assertTrue(PasswordHashes.isLegacySha512(stored));
        assertTrue(hashes.verify("secret", stored));
        assertFalse(hashes.verify("other", stored));
        assertTrue(hashes.needsRehash(stored));
        assertFalse(PasswordHashes.isUnmigratedClientHash("secret", stored));
    }

    @Test
    void hashedPostAgainstUnmigratedSha512DoesNotCountAsPlaintextMigrate() {
        String stored = PasswordHashes.sha512Hex("secret");
        assertTrue(PasswordHashes.isUnmigratedClientHash(stored, stored));
        assertTrue(PasswordHashes.isUnmigratedClientHash(stored.toUpperCase(), stored));
        assertFalse(hashes.needsRehash(hashes.hash("secret")));
        assertTrue(hashes.needsRehash(stored));
        assertFalse(PasswordHashes.isUnmigratedClientHash("secret", stored));
    }

    @Test
    void leftoverPlaintextStoredStillComparesEquality() {
        assertTrue(hashes.verify("legacy", "legacy"));
        assertTrue(hashes.needsRehash("legacy"));
        assertFalse(hashes.verify("legacy", "other"));
    }

    @Test
    void nullBlankAndTooLong() {
        assertFalse(hashes.verify(null, hashes.hash("x")));
        assertFalse(hashes.verify("x", null));
        assertFalse(hashes.verify("x", "   "));
        assertTrue(hashes.needsRehash(null));
        assertFalse(hashes.isTooLong("x".repeat(256)));
        assertTrue(hashes.isTooLong("x".repeat(257)));
    }
}
