package com.ravenherz.cse.util;

import com.ravenherz.cse.dal.dao.AccountService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountProfileTest {

    private final PasswordHashes hashes = new PasswordHashes();

    @Test
    void viewOmitsSecretFields() {
        AccountData data = new AccountData("ada", "secret-hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER);
        data.setShownName("Ada");
        data.setBio("Writes software.");
        data.setAvatar("data:image/jpeg;base64,abc");
        Map<String, Object> view = AccountProfile.view(data);
        assertEquals("ada", view.get("login"));
        assertEquals("ada@example.com", view.get("emailAddress"));
        assertEquals("Ada", view.get("shownName"));
        assertEquals("Writes software.", view.get("bio"));
        assertEquals("data:image/jpeg;base64,abc", view.get("avatar"));
        assertEquals("ACTIVE_USER", view.get("level"));
        assertEquals(false, view.get("admin"));
        assertFalse(view.containsKey("hash"));
        assertFalse(view.containsKey("sessions"));
        assertFalse(view.containsKey("activationToken"));
    }

    @Test
    void applyUpdatesPublicFieldsAndLeavesPassword() {
        AccountData data = new AccountData("ada", "keep-hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER);
        Map<String, String> json = new HashMap<>();
        json.put("ACCOUNT_EMAIL", "ada@example.net");
        json.put("ACCOUNT_SHOWN_NAME", "Ada Lovelace");
        json.put("ACCOUNT_BIO", "Notes.");
        assertNull(AccountProfile.apply(data, json, hashes, mock(AccountService.class)));
        assertEquals("ada@example.net", data.getEmailAddress());
        assertEquals("Ada Lovelace", data.getShownName());
        assertEquals("Notes.", data.getBio());
        assertEquals("keep-hash", data.getHash());
        assertEquals("ada", data.getLogin());
    }

    @Test
    void applyClearsAvatarWhenBlank() {
        AccountData data = new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER);
        data.setAvatar("data:image/jpeg;base64,abc");
        Map<String, String> json = baseJson();
        json.put("ACCOUNT_AVATAR", "");
        assertNull(AccountProfile.apply(data, json, hashes, mock(AccountService.class)));
        assertNull(data.getAvatar());
    }

    @Test
    void applyRejectsTakenEmail() {
        AccountData data = new AccountData("ada", "hash", "ada@example.com",
                SecurityLevel.ACTIVE_USER);
        AccountEntity other = new AccountEntity(new AccountData("bob", "hash", "bob@example.com",
                SecurityLevel.ACTIVE_USER));
        AccountService accounts = mock(AccountService.class);
        when(accounts.getByEmail("bob@example.com")).thenReturn(other);
        Map<String, String> json = baseJson();
        json.put("ACCOUNT_EMAIL", "bob@example.com");
        assertEquals("Email is already taken", AccountProfile.apply(data, json, hashes, accounts));
        assertEquals("ada@example.com", data.getEmailAddress());
    }

    @Test
    void applyPasswordRequiresCurrent() {
        AccountData data = new AccountData("ada", hashes.hash("old-secret"), "ada@example.com",
                SecurityLevel.ACTIVE_USER);
        Map<String, String> json = baseJson();
        json.put("ACCOUNT_PASSWORD", "new-secret");
        json.put("ACCOUNT_PASSWORD_RETYPE", "new-secret");
        assertEquals("Current password is required", AccountProfile.apply(data, json, hashes,
                mock(AccountService.class)));
        json.put("ACCOUNT_PASSWORD_CURRENT", "wrong");
        assertEquals("Current password is wrong", AccountProfile.apply(data, json, hashes,
                mock(AccountService.class)));
        json.put("ACCOUNT_PASSWORD_CURRENT", "old-secret");
        assertNull(AccountProfile.apply(data, json, hashes, mock(AccountService.class)));
        assertTrue(hashes.verify("new-secret", data.getHash()));
    }

    @Test
    void ownerIsAdmin() {
        AccountData data = new AccountData("ada", "hash", "ada@example.com", SecurityLevel.OWNER);
        assertEquals(true, AccountProfile.view(data).get("admin"));
    }

    private static Map<String, String> baseJson() {
        Map<String, String> json = new HashMap<>();
        json.put("ACCOUNT_EMAIL", "ada@example.com");
        json.put("ACCOUNT_SHOWN_NAME", "");
        json.put("ACCOUNT_BIO", "");
        return json;
    }
}
