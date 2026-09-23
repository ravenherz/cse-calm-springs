package com.ravenherz.cse.util.video;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.IntConsumer;

public interface VideoTranscoder {

    void recode(Path input, Path output) throws IOException;

    default void recode(Path input, Path output, Double durationSeconds, IntConsumer percent) throws IOException {
        recode(input, output, durationSeconds, percent, VideoUploadOptions.defaults());
    }

    default void recode(Path input, Path output, Double durationSeconds, IntConsumer percent,
            VideoUploadOptions options) throws IOException {
        recode(input, output);
        if (percent != null) {
            percent.accept(100);
        }
    }

    void poster(Path input, Path output) throws IOException;
}
