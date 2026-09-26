package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Transcode queue for the editor page. The WAR fills the rows.
 */
public interface TranscodeQueue {

    QueueSnapshot snapshot(HttpServletRequest request);

    record QueueSnapshot(int running, int queued, int failed, int ready,
            int workerRunning, int workerWaiting, List<Item> videos, int page, int size, long total) {

        public QueueSnapshot(int running, int queued, int failed, int ready,
                int workerRunning, int workerWaiting, List<Item> videos) {
            this(running, queued, failed, ready, workerRunning, workerWaiting, videos,
                    1, 20, videos == null ? 0 : videos.size());
        }
    }

    record Item(String id, String status, int percent, String error, String preview,
            String author, String sizeIn, String sizeOut, String fileName) {

        public String statusLabel() {
            return switch (status == null ? "" : status) {
                case "in-progress", "processing" -> "In progress";
                case "done", "ready" -> "Done";
                case "failed" -> "Failed";
                default -> "Queued";
            };
        }
    }
}
