package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Transcode queue for the editor page. The WAR fills the rows.
 */
public interface TranscodeQueue {

    QueueSnapshot snapshot(HttpServletRequest request);

    record QueueSnapshot(int running, int queued, int failed, int ready,
            int workerRunning, int workerWaiting, List<Item> videos) {
    }

    record Item(String id, String status, int percent, String error, String preview,
            String author, String sizeIn, String sizeOut, String fileName) {
    }
}
