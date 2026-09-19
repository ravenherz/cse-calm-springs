package com.ravenherz.cse.util.video;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class VideoProgress {

    public static final String QUEUED = "queued";
    public static final String PROCESSING = "processing";
    public static final String READY = "ready";
    public static final String FAILED = "failed";

    private static final long KEEP_MS = TimeUnit.MINUTES.toMillis(15);

    public record View(String id, String status, int percent, String error, String preview, String sizeLabel) {
        public View withPreview(String href) {
            return new View(id, status, percent, error, href, sizeLabel);
        }
    }

    private final ConcurrentHashMap<String, Entry> byId = new ConcurrentHashMap<>();

    public void queued(ObjectId id) {
        if (id == null) {
            return;
        }
        prune();
        byId.compute(id.toHexString(), (key, existing) -> {
            if (existing != null && (PROCESSING.equals(existing.status) || READY.equals(existing.status))) {
                return existing;
            }
            return Entry.queued(key);
        });
    }

    public void start(ObjectId id) {
        if (id == null) {
            return;
        }
        prune();
        byId.compute(id.toHexString(), (key, existing) -> {
            Entry entry = existing == null ? Entry.queued(key) : existing;
            if (READY.equals(entry.status) || FAILED.equals(entry.status)) {
                entry = Entry.queued(key);
            }
            entry.status = PROCESSING;
            entry.percent = Math.max(1, entry.percent);
            entry.touch();
            return entry;
        });
    }

    public void percent(ObjectId id, int percent) {
        if (id == null) {
            return;
        }
        Entry entry = byId.get(id.toHexString());
        if (entry == null || READY.equals(entry.status) || FAILED.equals(entry.status)) {
            return;
        }
        entry.status = PROCESSING;
        entry.percent = Math.max(entry.percent, Math.max(0, Math.min(99, percent)));
        entry.touch();
    }

    public void ready(ObjectId id, String previewPath, String sizeLabel) {
        if (id == null) {
            return;
        }
        Entry entry = byId.computeIfAbsent(id.toHexString(), Entry::queued);
        entry.status = READY;
        entry.percent = 100;
        entry.error = null;
        entry.preview = blankToNull(previewPath);
        entry.sizeLabel = blankToNull(sizeLabel);
        entry.touch();
    }

    public void failed(ObjectId id, String error) {
        if (id == null) {
            return;
        }
        Entry entry = byId.computeIfAbsent(id.toHexString(), Entry::queued);
        entry.status = FAILED;
        entry.error = blankToNull(error);
        entry.touch();
    }

    public View view(ObjectId id) {
        if (id == null) {
            return null;
        }
        prune();
        Entry entry = byId.get(id.toHexString());
        return entry == null ? null : entry.toView();
    }

    public List<View> views() {
        prune();
        List<View> out = new ArrayList<>();
        for (Entry entry : byId.values()) {
            out.add(entry.toView());
        }
        out.sort(Comparator.comparingInt((View view) -> rank(view.status()))
                .thenComparingInt((View view) -> PROCESSING.equals(view.status()) ? -view.percent() : 0)
                .thenComparing(View::id));
        return out;
    }

    private static int rank(String status) {
        if (PROCESSING.equals(status)) {
            return 0;
        }
        if (QUEUED.equals(status)) {
            return 1;
        }
        if (FAILED.equals(status)) {
            return 2;
        }
        return 3;
    }

    private void prune() {
        long cutoff = System.currentTimeMillis() - KEEP_MS;
        byId.entrySet().removeIf(item -> {
            Entry entry = item.getValue();
            return entry != null
                    && (READY.equals(entry.status) || FAILED.equals(entry.status))
                    && entry.updatedAt < cutoff;
        });
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static final class Entry {
        final String id;
        volatile String status;
        volatile int percent;
        volatile String error;
        volatile String preview;
        volatile String sizeLabel;
        volatile long updatedAt;

        private Entry(String id) {
            this.id = id;
            this.status = QUEUED;
            this.updatedAt = System.currentTimeMillis();
        }

        static Entry queued(String id) {
            return new Entry(id);
        }

        void touch() {
            updatedAt = System.currentTimeMillis();
        }

        View toView() {
            return new View(id, status, percent, error, preview, sizeLabel);
        }
    }
}
