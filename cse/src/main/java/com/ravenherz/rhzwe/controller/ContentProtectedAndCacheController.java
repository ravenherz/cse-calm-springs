package com.ravenherz.rhzwe.controller;

import com.ravenherz.rhzwe.dal.EntityUtils;
import com.ravenherz.rhzwe.dal.dto.ResourceEntity;
import com.ravenherz.rhzwe.dal.dto.basic.enums.AccessType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Controller
@Scope(value = "singleton")
public class ContentProtectedAndCacheController extends AbstractController {

    private static final Map<String, String> cache = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentProtectedAndCacheController.class);

    private static class ContentCacheOperator {

        private static File getRoot() {
            String root = "/var/cse";
            try {
                return ResourceUtils.getFile(root);
            } catch (FileNotFoundException ex) {
                LOGGER.info("File not found ''");
                throw new Error("Error in initialization");
            }
        }

        private static @NotNull File getFile(ResourceEntity resourceEntity) {
            return new File(getRoot().getAbsolutePath() +
                    "/content-cache" + resourceEntity.getResourceData().getPathProtected());
        }

        public static void prepareFile(ResourceEntity resourceEntity) throws IOException {
            File file = getFile(resourceEntity);
            if (!file.exists()) {
                LOGGER.info("Writing and caching file binary data to %s".formatted(file.getAbsolutePath()));
                cache.put(resourceEntity.getResourceData().getPathPublic(), resourceEntity.getResourceData().getPathProtected());
                FileUtils.writeByteArrayToFile(file, resourceEntity.getRawBytes());
            } else {
                cache.put(resourceEntity.getResourceData().getPathPublic(), resourceEntity.getResourceData().getPathProtected());
                LOGGER.info("Cached: %s".formatted(file.getAbsolutePath()));
            }
        }
    }

    @RequestMapping("/content-protected/**")
    public @ResponseBody byte[] getResource(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        ResourceEntity resourceEntity = serviceProvider.getResourceService()
                .getByPublicPath(getResourceUri(request));
        if (resourceEntity == null || !EntityUtils.isAccessible(resourceEntity, AccessType.ACCESS_READ,
                getAccessor(request, response))) {
            response.sendRedirect(
                    request.getContextPath() + "/content-public/rhz-we-core/images/no-image.jpg");
        } else {
            if (cache.containsKey(resourceEntity.getResourceData().getPathPublic())) {
                LOGGER.info("Entry already in cache: %s".formatted(resourceEntity.getResourceData().getPathProtected()));
            } else {
                LOGGER.info("No cache entry for %s".formatted(resourceEntity.getResourceData().getPathProtected()));
                ContentCacheOperator.prepareFile(resourceEntity);
            }
            response.sendRedirect(request.getContextPath() + "/content-cache" + resourceEntity.getResourceData().getPathProtected());
        }
        return new byte[1];
    }

    private String getResourceUri(HttpServletRequest request) {
        return request.getRequestURI()
                .replaceFirst(request.getContextPath(),"")
                .replaceFirst("/content-protected", "");
    }

}
