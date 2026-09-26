package com.ravenherz.cse.scripting;

public enum ScriptRunStatus {
    QUEUED("queued", "Queued"),
    RUNNING("running", "Running"),
    DONE("done", "Done"),
    FAILED("failed", "Failed");

    private final String token;
    private final String label;

    ScriptRunStatus(String token, String label) {
        this.token = token;
        this.label = label;
    }

    public String token() {
        return token;
    }

    public String label() {
        return label;
    }
}
