package com.ravenherz.cse.util.video;

import com.ravenherz.cse.dal.EntityId;

public final class VideoTranscodeRecords {

    private VideoTranscodeRecords() {
    }

    public static VideoTranscodeEntity apply(VideoTranscodeEntity existing, EntityId resourceId,
            VideoTranscodeStatus status, int percent, String error) {
        VideoTranscodeEntity row = existing == null ? new VideoTranscodeEntity() : existing;
        row.setResourceId(resourceId);
        row.setStatus(status == null ? VideoTranscodeStatus.QUEUED : status);
        row.setPercent(percent);
        row.setError(blank(error));
        row.setUpdatedAt(System.currentTimeMillis());
        return row;
    }

    private static String blank(String error) {
        if (error == null || error.isBlank()) {
            return null;
        }
        return error.trim();
    }
}
