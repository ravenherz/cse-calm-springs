package com.ravenherz.cse.engine.video;

import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.install.SiteReady;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class VideoTranscodeQueue implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoTranscodeQueue.class);

    private final ResourceService resources;
    private final VideoTranscodeJob job;
    private final VideoProgress progress;
    private final ObjectProvider<SiteReady> siteReady;
    private final ExecutorService executor;
    private boolean resumeOnStart = true;

    @Autowired
    public VideoTranscodeQueue(ObjectProvider<ResourceService> resources, VideoTranscodeJob job,
            ObjectProvider<VideoProgress> progress, ObjectProvider<SiteReady> siteReady) {
        this.resources = resources == null ? null : resources.getIfAvailable();
        this.job = job;
        this.progress = progress == null ? null : progress.getIfAvailable();
        this.siteReady = siteReady;
        this.executor = newWorker();
    }

    VideoTranscodeQueue(ResourceService resources, VideoTranscodeJob job, ExecutorService executor,
            ObjectProvider<SiteReady> siteReady) {
        this.resources = resources;
        this.job = job;
        this.progress = null;
        this.siteReady = siteReady;
        this.executor = executor;
    }

    @Value("${cse.video.transcode.resume:true}")
    void setResumeOnStart(boolean resumeOnStart) {
        this.resumeOnStart = resumeOnStart;
    }

    @Override
    public void run(ApplicationArguments args) {
        resume();
    }

    void resume() {
        if (!resumeOnStart || resources == null || job == null) {
            return;
        }
        SiteReady ready = siteReady == null ? null : siteReady.getIfAvailable();
        if (ready != null && !ready.isConfigured()) {
            LOGGER.info("Skip video transcode resume: engine is not configured");
            return;
        }
        try {
            List<ObjectId> ids = resources.listProcessingVideoIds();
            if (ids == null || ids.isEmpty()) {
                return;
            }
            LOGGER.info("Resuming {} video transcode(s)", ids.size());
            for (ObjectId id : ids) {
                enqueue(id);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not resume video transcodes: {}", e.getMessage());
        }
    }

    public void enqueue(ObjectId id) {
        if (id == null || job == null) {
            return;
        }
        if (progress != null) {
            progress.queued(id);
        }
        executor.execute(() -> {
            try {
                job.run(id);
            } catch (Exception e) {
                LOGGER.warn("Video transcode task failed for {}: {}", id, e.getMessage());
            }
        });
    }

    public int workerRunning() {
        if (executor instanceof ThreadPoolExecutor pool) {
            return pool.getActiveCount();
        }
        return 0;
    }

    public int workerWaiting() {
        if (executor instanceof ThreadPoolExecutor pool) {
            return pool.getQueue().size();
        }
        return 0;
    }

    private static ThreadPoolExecutor newWorker() {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable, "cse-video-transcode");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @PreDestroy
    void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}
