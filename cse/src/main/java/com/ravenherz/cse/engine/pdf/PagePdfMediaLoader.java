package com.ravenherz.cse.engine.pdf;

import com.ravenherz.cse.pdf.PagePdfRenderer;

import com.ravenherz.cse.dal.EntityAccess;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.AccessType;
import com.ravenherz.cse.util.io.CseDisk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;

/**
 * Loads content-protected images for article PDF rendering.
 */
@Component
public class PagePdfMediaLoader implements PagePdfRenderer.MediaLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PagePdfMediaLoader.class);

    private final ServiceProvider services;

    public PagePdfMediaLoader(ServiceProvider services) {
        this.services = services;
    }

    @Override
    public byte[] load(String src, AccountEntity accessor) {
        if (services == null) {
            return null;
        }
        String publicPath = PagePdfRenderer.publicPathFromSrc(src);
        if (publicPath.isEmpty()) {
            return null;
        }
        ResourceService resources = services.getResourceService();
        if (resources == null) {
            return null;
        }
        ResourceEntity entity = resources.getByPublicPath(publicPath);
        if (entity == null || !EntityAccess.isAccessible(entity, AccessType.ACCESS_READ, accessor)) {
            return null;
        }
        ResourceData data = entity.dataForPublicPath(publicPath);
        if (data == null) {
            return null;
        }
        byte[] cached = cachedBytes(data.getPathProtected());
        if (cached != null) {
            return cached;
        }
        byte[] inline = ResourceEntity.bytesOf(data);
        if (inline != null) {
            return inline;
        }
        if (data.isLargeFile() && data.getDataChunkIds() != null && !data.getDataChunkIds().isEmpty()) {
            return resources.getRawBytesFromChunks(data.getDataChunkIds());
        }
        return null;
    }

    private static byte[] cachedBytes(String pathProtected) {
        if (pathProtected == null || pathProtected.isBlank()) {
            return null;
        }
        try {
            var file = CseDisk.cachedMedia(pathProtected);
            if (file != null && file.isFile()) {
                return Files.readAllBytes(file.toPath());
            }
        } catch (Exception ex) {
            LOGGER.debug("PDF media cache miss for {}: {}", pathProtected, ex.getMessage());
        }
        return null;
    }
}
