package com.ravenherz.cse.transfer;

import java.io.IOException;

public final class CseSiteImportException extends IOException {

    public CseSiteImportException(String message) {
        super(message);
    }

    public CseSiteImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
