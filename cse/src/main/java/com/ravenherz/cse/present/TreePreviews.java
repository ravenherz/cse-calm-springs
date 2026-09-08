package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import com.ravenherz.cse.util.imaging.JpegImages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 64×64 JPEG thumbs for the editor tree. Stored on {@link ResourceGroupIndex}.
 */
public final class TreePreviews {

    public static final int SIZE = 64;
    static final float QUALITY = 0.72f;

    private TreePreviews() {
    }

    public static byte[] jpeg(byte[] source) {
        if (source == null || source.length == 0) {
            return null;
        }
        try {
            return JpegImages.squareThumb(source, SIZE, QUALITY).bytes();
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] jpeg(ResourceEntity resource) {
        if (resource == null) {
            return null;
        }
        byte[] source = resource.getPreviewBytes();
        if (source == null && resource.getResourceData() != null
                && resource.getResourceData().getType() == ResourceType.IMAGE) {
            source = resource.getRawBytes();
        }
        return jpeg(source);
    }

    public static byte[] jpegFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            return jpeg(Files.readAllBytes(file));
        } catch (IOException e) {
            return null;
        }
    }
}
