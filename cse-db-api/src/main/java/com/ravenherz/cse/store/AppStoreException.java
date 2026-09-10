package com.ravenherz.cse.store;

public final class AppStoreException extends RuntimeException {

    private final int status;

    public AppStoreException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
