package com.ravenherz.cse.controller;

import com.ravenherz.cse.util.io.CseDisk;
import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Controller
@Scope(value = "singleton")
public class ContentProtectedAndCacheController extends AbstractController {

    private static Map<String, String> cache = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentProtectedAndCacheController.class);

    private static class ContentCacheOperator {

        private static @NotNull File getFile(ResourceData data) {
            return CseDisk.cachedMedia(data.getPathProtected());
        }

        public static void prepareFile(ResourceData data,
                Supplier<byte[]> inlineBytesSupplier,
                Supplier<byte[]> chunkBytesSupplier) throws IOException {
            File file = getFile(data);
            if (!file.exists()) {
                LOGGER.info("Writing and caching file binary data to %s".formatted(file.getAbsolutePath()));
                cache.put(data.getPathPublic(), data.getPathProtected());
                byte[] bytes = inlineBytesSupplier == null ? null : inlineBytesSupplier.get();
                if (bytes == null && chunkBytesSupplier != null) {
                    bytes = chunkBytesSupplier.get();
                }
                FileUtils.writeByteArrayToFile(file, bytes);
            } else {
                cache.put(data.getPathPublic(), data.getPathProtected());
                LOGGER.debug("Cached: %s".formatted(file.getAbsolutePath()));
            }
        }
    }

    @RequestMapping("/content-protected/**")
    public @ResponseBody byte[] getResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        ResourceEntity resourceEntity = serviceProvider.getResourceService()
                .getByPublicPath(getResourceUri(request));
        if (resourceEntity == null || !EntityAccess.isAccessible(resourceEntity, AccessType.ACCESS_READ,
                getAccessor(request, response))) {
            response.sendRedirect(
                    request.getContextPath() + "/content-public/cse-core/images/no-image.jpg");
        } else {
            ResourceData data = resourceEntity.dataForPublicPath(getResourceUri(request));
            if (data == null) {
                response.sendRedirect(
                        request.getContextPath() + "/content-public/cse-core/images/no-image.jpg");
                return new byte[1];
            }
            if (cache.containsKey(data.getPathPublic())) {
                LOGGER.debug("Entry already in cache: %s".formatted(data.getPathProtected()));
            } else {
                LOGGER.debug("No cache entry for %s".formatted(data.getPathProtected()));
                Supplier<byte[]> chunkSupplier = null;
                if (data.isLargeFile() && data.getDataChunkIds() != null) {
                    LOGGER.debug("Large file detected with " + data.getDataChunkIds().size() + " chunks");
                    chunkSupplier = () -> {
                        byte[] bytes = serviceProvider.getResourceService().getRawBytesFromChunks(
                                data.getDataChunkIds());
                        LOGGER.debug("Retrieved " + (bytes != null ? bytes.length : 0) + " bytes from chunks");
                        return bytes;
                    };
                }
                ContentCacheOperator.prepareFile(data, () -> ResourceEntity.bytesOf(data), chunkSupplier);
            }
            String protectedPath = data.getPathProtected();
            LOGGER.debug("Protected path for resource: " + protectedPath);
            if (protectedPath == null) {
                LOGGER.error("Protected path is null for resource: " + data.getPathPublic());
                response.sendRedirect(request.getContextPath() + "/?error=500");
                return new byte[1];
            }
            response.sendRedirect(request.getContextPath() + "/content-cache" + protectedPath);
        }
        return new byte[1];
    }

    public void invalidateCacheForResource(String publicPath) {
        String protectedPath = cache.remove(publicPath);
        if (protectedPath != null) {
            try {
                File cachedFile = CseDisk.cachedMedia(protectedPath);
                if (cachedFile.exists()) {
                    cachedFile.delete();
                    LOGGER.info("Deleted cached file: " + cachedFile.getAbsolutePath());
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to delete cached file for: " + publicPath, e);
            }
        }
    }

    private String getResourceUri(HttpServletRequest request) {
        return request.getRequestURI()
                .replaceFirst(request.getContextPath(),"")
                .replaceFirst("/content-protected", "");
    }

}
