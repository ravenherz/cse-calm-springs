package com.ravenherz.cse.engine.video;

import com.ravenherz.cse.util.video.FfmpegBinaries;
import com.ravenherz.cse.util.video.FfmpegVideoTranscoder;
import com.ravenherz.cse.util.video.VideoProbe;
import com.ravenherz.cse.util.video.VideoTranscoder;
import com.ravenherz.cse.util.video.VideoUploadOptions;
import com.ravenherz.cse.util.video.VideoWork;

import com.ravenherz.cse.constants.SettingKeys;
import com.ravenherz.cse.controller.ContentProtectedAndCacheController;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.engine.util.Settings;
import com.ravenherz.cse.util.imaging.ImageUploadOptions;
import com.ravenherz.cse.util.imaging.JpegImages;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class VideoTranscodeJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoTranscodeJob.class);

    private final ResourceService resources;
    private final VideoTranscoder transcoder;
    private final ContentProtectedAndCacheController cache;
    private final ResourceGroupIndex index;
    private final Settings settings;
    private final Inspector inspector;
    private final VideoProgress progress;

    @FunctionalInterface
    interface Inspector {
        VideoProbe.Info inspect(Path file) throws IOException;
    }

    @Autowired
    public VideoTranscodeJob(ObjectProvider<ResourceService> resources,
            ObjectProvider<ContentProtectedAndCacheController> cache,
            ObjectProvider<ResourceGroupIndex> index,
            ObjectProvider<Settings> settings,
            ObjectProvider<VideoProgress> progress) {
        this(resources, cache, index, settings, new FfmpegVideoTranscoder(), VideoProbe::probe, progress);
    }

    VideoTranscodeJob(ObjectProvider<ResourceService> resources,
            ObjectProvider<ContentProtectedAndCacheController> cache,
            ObjectProvider<ResourceGroupIndex> index,
            ObjectProvider<Settings> settings,
            VideoTranscoder transcoder) {
        this(resources, cache, index, settings, transcoder, VideoProbe::probe, null);
    }

    VideoTranscodeJob(ObjectProvider<ResourceService> resources,
            ObjectProvider<ContentProtectedAndCacheController> cache,
            ObjectProvider<ResourceGroupIndex> index,
            ObjectProvider<Settings> settings,
            VideoTranscoder transcoder,
            Inspector inspector) {
        this(resources, cache, index, settings, transcoder, inspector, null);
    }

    VideoTranscodeJob(ObjectProvider<ResourceService> resources,
            ObjectProvider<ContentProtectedAndCacheController> cache,
            ObjectProvider<ResourceGroupIndex> index,
            ObjectProvider<Settings> settings,
            VideoTranscoder transcoder,
            Inspector inspector,
            ObjectProvider<VideoProgress> progress) {
        this.resources = resources == null ? null : resources.getIfAvailable();
        this.cache = cache == null ? null : cache.getIfAvailable();
        this.index = index == null ? null : index.getIfAvailable();
        this.settings = settings == null ? null : settings.getIfAvailable();
        this.transcoder = transcoder == null ? new FfmpegVideoTranscoder() : transcoder;
        this.inspector = inspector == null ? VideoProbe::probe : inspector;
        this.progress = progress == null ? null : progress.getIfAvailable();
    }

    static VideoTranscodeJob of(ResourceService resources, VideoTranscoder transcoder,
            ContentProtectedAndCacheController cache, ResourceGroupIndex index, Settings settings) {
        return of(resources, transcoder, cache, index, settings, VideoProbe::probe);
    }

    static VideoTranscodeJob of(ResourceService resources, VideoTranscoder transcoder,
            ContentProtectedAndCacheController cache, ResourceGroupIndex index, Settings settings,
            Inspector inspector) {
        return new VideoTranscodeJob(of(resources), of(cache), of(index), of(settings), transcoder, inspector);
    }

    public void run(ObjectId id) {
        if (id == null || resources == null) {
            return;
        }
        BasicEntity found = resources.getById(ResourceEntity.class, id);
        if (!(found instanceof ResourceEntity resource) || resource.getResourceData() == null
                || resource.getResourceData().getType() != ResourceType.VIDEO
                || !VideoStatus.processing(resource.getResourceData())) {
            return;
        }
        Path source = null;
        Path recoded = null;
        Path poster = null;
        try {
            start(id);
            source = VideoWork.temp("src-", suffix(resource));
            recoded = VideoWork.temp("out-", ".mp4");
            resources.writeToFile(resource.getResourceData(), source);
            percent(id, 5);
            LOGGER.info("Video {}: copied source, loading FFmpeg", id);
            FfmpegBinaries.ffprobe();
            percent(id, 6);
            LOGGER.info("Video {}: probing {}", id, source.getFileName());
            VideoProbe.Info info = inspector.inspect(source);
            applyProbe(resource.getResourceData(), info);
            Double duration = durationOf(info, resource.getResourceData());
            percent(id, 8);
            VideoUploadOptions options = uploadOptions();
            LOGGER.info("Video {}: recoding duration={} {}x{} q={}", id, duration,
                    options.maxWidth(), options.maxHeight(), options.qualityFactor());
            transcoder.recode(source, recoded, duration, encoded -> percent(id, encodePercent(encoded)), options);
            VideoWork.deleteQuietly(source);
            source = null;
            applyProbe(resource.getResourceData(), inspector.inspect(recoded));
            percent(id, 96);
            try {
                poster = VideoWork.temp("poster-", ".jpg");
                transcoder.poster(recoded, poster);
            } catch (Exception e) {
                LOGGER.warn("Video poster failed for {}: {}", id, e.getMessage());
                VideoWork.deleteQuietly(poster);
                poster = null;
            }
            percent(id, 98);
            replaceBytes(resource, recoded, poster);
            VideoStatus.set(resource.getResourceData(), VideoStatus.READY);
            resources.replace(resource);
            invalidate(resource);
            if (index != null) {
                index.rebuild();
            }
            ready(resource);
        } catch (Exception e) {
            LOGGER.warn("Video transcode failed for {}: {}", id, e.getMessage(), e);
            try {
                BasicEntity latest = resources.getById(ResourceEntity.class, id);
                if (latest instanceof ResourceEntity entity && entity.getResourceData() != null) {
                    VideoStatus.fail(entity.getResourceData(), e.getMessage());
                    resources.replace(entity);
                    if (index != null) {
                        index.rebuild();
                    }
                }
            } catch (Exception persist) {
                LOGGER.warn("Could not mark video failed {}: {}", id, persist.getMessage());
            }
            fail(id, e.getMessage());
        } finally {
            VideoWork.deleteQuietly(source);
            VideoWork.deleteQuietly(recoded);
            VideoWork.deleteQuietly(poster);
        }
    }

    private void replaceBytes(ResourceEntity resource, Path recoded, Path poster) throws IOException {
        ResourceData data = resource.getResourceData();
        VideoStatus.rememberSourceSize(data);
        String publicPath = data.getPathPublic();
        resources.deleteStoredContent(data);
        data.setType(ResourceType.VIDEO);
        data.setPathPublic(publicPath);
        resources.fillFromFile(data, recoded);
        if (poster != null && Files.isRegularFile(poster)) {
            replacePoster(resource, Files.readAllBytes(poster));
        }
    }

    private void replacePoster(ResourceEntity resource, byte[] jpegBytes) {
        ResourceData data = resource.getResourceData();
        if (resource.getPreviewData() != null) {
            String previewPath = resource.getPreviewData().getPathPublic();
            resources.deleteStoredContent(resource.getPreviewData());
            if (cache != null && previewPath != null) {
                cache.invalidateCacheForResource(previewPath);
            }
        }
        try {
            ImageUploadOptions options = ImageUploadOptions.from(settings);
            JpegImages.Encoded jpeg = JpegImages.toJpeg(jpegBytes, options.previewMaxWidth(),
                    options.qualityFactor());
            String resourceId = fileStem(data.getFileName());
            String userId = loginOf(data.getPathPublic());
            String previewPublic = String.format("/%s/res/%s/%s.poster.low-res.jpg",
                    userId, ResourceType.VIDEO.getPath(), resourceId);
            ResourceData preview = new ResourceData();
            preview.setType(ResourceType.IMAGE);
            preview.setPathPublic(previewPublic);
            preview.setPathProtected(uniqueProtected(resourceId + ".poster.low-res", "jpg"));
            preview.addMetadata("width", String.valueOf(jpeg.width()));
            preview.addMetadata("height", String.valueOf(jpeg.height()));
            Path stored = null;
            try {
                stored = writeTempJpeg(jpeg.bytes());
                resources.fillFromFile(preview, stored);
                resource.setPreviewData(preview);
            } finally {
                VideoWork.deleteQuietly(stored);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not store video poster: {}", e.getMessage());
            resource.setPreviewData(null);
        }
    }

    private Path writeTempJpeg(byte[] bytes) throws IOException {
        Path temp = VideoWork.temp("poster-store-", ".jpg");
        Files.write(temp, bytes);
        return temp;
    }

    private void invalidate(ResourceEntity resource) {
        if (cache == null || resource.getResourceData() == null) {
            return;
        }
        cache.invalidateCacheForResource(resource.getResourceData().getPathPublic());
        if (resource.getPreviewData() != null) {
            cache.invalidateCacheForResource(resource.getPreviewData().getPathPublic());
        }
    }

    private String uniqueProtected(String resourceId, String extension) {
        for (int i = 0; i < 10; i++) {
            String path = "/" + randomSegment() + "/" + resourceId + "." + extension;
            if (resources.getByProtectedPath(path) == null) {
                return path;
            }
        }
        return "/" + randomSegment() + "/" + resourceId + "." + extension;
    }

    private static String randomSegment() {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder out = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 24; i++) {
            if (i > 0 && i % 8 == 0) {
                out.append('/');
            }
            out.append(chars.charAt(random.nextInt(chars.length())));
        }
        return out.toString();
    }

    private static void applyProbe(ResourceData data, VideoProbe.Info info) {
        if (data == null || info == null) {
            return;
        }
        data.addMetadata("width", String.valueOf(info.width()));
        data.addMetadata("height", String.valueOf(info.height()));
        if (info.durationSeconds() != null) {
            data.addMetadata("duration", String.valueOf(Math.max(0, Math.round(info.durationSeconds()))));
        }
    }

    private void start(ObjectId id) {
        if (progress != null) {
            progress.start(id);
        }
    }

    private void percent(ObjectId id, int value) {
        if (progress != null) {
            progress.percent(id, value);
        }
    }

    private void ready(ResourceEntity resource) {
        if (progress == null || resource == null || resource.getId() == null
                || resource.getResourceData() == null) {
            return;
        }
        String preview = resource.getPreviewData() == null ? null : resource.getPreviewData().getPathPublic();
        progress.ready(resource.getId(), preview, resource.getResourceData().getSizeLabel());
    }

    private void fail(ObjectId id, String message) {
        if (progress != null) {
            progress.failed(id, message);
        }
    }

    static int encodePercent(int ffmpegPercent) {
        int clamped = Math.max(0, Math.min(100, ffmpegPercent));
        return 8 + (int) Math.round(clamped * 0.87);
    }

    private static Double durationOf(VideoProbe.Info info, ResourceData data) {
        if (info != null && info.durationSeconds() != null && info.durationSeconds() > 0) {
            return info.durationSeconds();
        }
        if (data == null || data.getMetadata() == null) {
            return null;
        }
        String raw = data.getMetadata().get("duration");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private VideoUploadOptions uploadOptions() {
        if (settings == null) {
            return VideoUploadOptions.defaults();
        }
        return VideoUploadOptions.from(
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD, SettingKeys.KEY_MAX_WIDTH),
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD, SettingKeys.KEY_MAX_HEIGHT),
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD, SettingKeys.KEY_QUALITY_FACTOR),
                settings.getValue(SettingKeys.CONTEXT_DATASOURCE_VIDEO_UPLOAD, SettingKeys.KEY_AUDIO_BITRATE));
    }

    private static String suffix(ResourceEntity resource) {
        String ext = "";
        if (resource.getResourceData() != null && resource.getResourceData().getMetadata() != null) {
            String source = resource.getResourceData().getMetadata().get(VideoStatus.SOURCE_EXT_KEY);
            if (source != null && !source.isBlank()) {
                ext = source.trim().toLowerCase(Locale.ROOT);
            }
        }
        if (ext.isEmpty()) {
            String name = resource.getResourceData() == null ? "" : resource.getResourceData().getFileName();
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            }
        }
        if (ext.isEmpty()) {
            ext = "bin";
        }
        return "." + ext;
    }

    private static String fileStem(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "video";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String loginOf(String pathPublic) {
        if (pathPublic == null || pathPublic.isBlank()) {
            return "res";
        }
        String path = pathPublic.startsWith("/") ? pathPublic.substring(1) : pathPublic;
        int slash = path.indexOf('/');
        return slash > 0 ? path.substring(0, slash) : path;
    }

    private static <T> ObjectProvider<T> of(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }
}
