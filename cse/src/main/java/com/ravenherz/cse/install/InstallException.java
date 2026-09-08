package com.ravenherz.cse.install;

public final class InstallException extends RuntimeException {

    private final int status;

    public InstallException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
