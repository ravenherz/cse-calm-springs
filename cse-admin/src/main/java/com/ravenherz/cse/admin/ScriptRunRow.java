package com.ravenherz.cse.admin;

/**
 * One row on the script runs tab.
 */
public final class ScriptRunRow {

    private final String scriptId;
    private final String status;
    private final String started;
    private final String message;

    public ScriptRunRow(String scriptId, String status, String started, String message) {
        this.scriptId = scriptId == null ? "" : scriptId;
        this.status = status == null ? "" : status;
        this.started = started == null ? "" : started;
        this.message = message == null ? "" : message;
    }

    public String getScriptId() {
        return scriptId;
    }

    public String getStatus() {
        return status;
    }

    public String getStarted() {
        return started;
    }

    public String getMessage() {
        return message;
    }
}
