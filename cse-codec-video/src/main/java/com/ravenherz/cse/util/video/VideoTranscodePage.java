package com.ravenherz.cse.util.video;

import java.util.List;

public record VideoTranscodePage(List<VideoTranscodeEntity> items, long total, int page, int size,
        long queued, long inProgress, long done, long failed) {

    public VideoTranscodePage {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static VideoTranscodePage empty(int page, int size) {
        return new VideoTranscodePage(List.of(), 0, page, size, 0, 0, 0, 0);
    }
}
