package com.ravenherz.cse.core.admin;

/**
 * A field the generic editor can show again after a rejected save.
 */
public record AdminFieldError(String field, String message) {
}
