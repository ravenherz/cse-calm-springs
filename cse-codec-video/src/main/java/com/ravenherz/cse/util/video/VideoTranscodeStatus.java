package com.ravenherz.cse.util.video;

/**
 * Queue states. A job moves Queued, then In progress, then Done. Failed is the error stop.
 */
public enum VideoTranscodeStatus {

    QUEUED("queued", "Queued", 1),
    IN_PROGRESS("in-progress", "In progress", 0),
    DONE("done", "Done", 3),
    FAILED("failed", "Failed", 2);

    private final String token;
    private final String label;
    private final int rank;

    VideoTranscodeStatus(String token, String label, int rank) {
        this.token = token;
        this.label = label;
        this.rank = rank;
    }

    public String token() {
        return token;
    }

    public String label() {
        return label;
    }

    public int rank() {
        return rank;
    }

    public static VideoTranscodeStatus fromToken(String token) {
        if (token == null || token.isBlank()) {
            return QUEUED;
        }
        String value = token.trim();
        if ("processing".equals(value) || IN_PROGRESS.token.equals(value)) {
            return IN_PROGRESS;
        }
        if ("ready".equals(value) || DONE.token.equals(value)) {
            return DONE;
        }
        if (FAILED.token.equals(value)) {
            return FAILED;
        }
        return QUEUED;
    }
}
