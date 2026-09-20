package com.ravenherz.cse.present;

import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.enums.ResourceType;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceTreeFileTest {

    @Test
    void videoFilesEmbedAsCseVideo() {
        ResourceTreeFile video = ResourceTreeFile.from(resource(ResourceType.VIDEO,
                "/u/res/video/clip.mp4"));
        assertTrue(video.isVideoFile());
        assertEquals("cse-video", video.embedTag());
        assertEquals(video.id(), video.embedId());
        assertTrue(video.isEmbeddable());
    }

    @Test
    void pdfFilesEmbedAsCseBinary() {
        ResourceTreeFile pdf = ResourceTreeFile.from(resource(ResourceType.BINARY,
                "/u/res/binaries/resume.pdf"));
        assertTrue(pdf.isBinaryFile());
        assertEquals("cse-binary", pdf.embedTag());
        assertEquals(pdf.id(), pdf.embedId());
        assertTrue(pdf.isEmbeddable());
    }

    @Test
    void imageFilesStillEmbedAsCseImage() {
        ResourceTreeFile image = ResourceTreeFile.from(resource(ResourceType.IMAGE,
                "/u/res/images/cover.jpg"));
        assertTrue(image.isImageFile());
        assertEquals("cse-image", image.embedTag());
    }

    @Test
    void audioFilesAreTypedForPlaylistDrop() {
        ResourceTreeFile audio = ResourceTreeFile.from(resource(ResourceType.AUDIO,
                "/u/res/audio/tide.mp3"));
        assertTrue(audio.isAudioFile());
        assertEquals("AUDIO", audio.resourceType());
        assertEquals("", audio.embedTag());
    }

    @Test
    void urlTemplatesEmbedAsCseUrlWithTemplateId() {
        ResourceTreeFile template = new ResourceTreeFile(
                "url-template-1", "youtube", "/editor/url-template/edit?id=1",
                ResourceTreeFile.Mark.URL_TEMPLATE)
                .withKey("1")
                .withEmbedId("youtube");
        assertTrue(template.isUrlTemplate());
        assertEquals("url-template", template.kind());
        assertEquals("cse-url", template.embedTag());
        assertEquals("youtube", template.embedId());
        assertTrue(template.isEmbeddable());
        assertTrue(template.canEdit());
        assertTrue(template.canRename());
    }

    private static ResourceEntity resource(ResourceType type, String path) {
        ResourceData data = new ResourceData();
        data.setType(type);
        data.setPathPublic(path);
        ResourceEntity entity = new ResourceEntity();
        entity.setId(new ObjectId());
        entity.setResourceData(data);
        return entity;
    }
}
