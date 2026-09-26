package com.ravenherz.cse.engine.scripting;

import com.ravenherz.cse.controller.ContentProtectedAndCacheController;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dao.AppService;
import com.ravenherz.cse.dal.dao.ItemService;
import com.ravenherz.cse.dal.dao.PlaylistService;
import com.ravenherz.cse.dal.dao.ResourceGroupService;
import com.ravenherz.cse.dal.dao.ResourceService;
import com.ravenherz.cse.dal.dao.ThemeService;
import com.ravenherz.cse.dal.dto.AppEntity;
import com.ravenherz.cse.dal.dto.ResourceEntity;
import com.ravenherz.cse.dal.dto.ResourceGroupEntity;
import com.ravenherz.cse.dal.dto.ThemeEntity;
import com.ravenherz.cse.dal.dto.basic.AppData;
import com.ravenherz.cse.dal.dto.basic.ResourceData;
import com.ravenherz.cse.dal.dto.basic.ResourceSizeHint;
import com.ravenherz.cse.dal.dto.basic.ThemeData;
import com.ravenherz.cse.present.ResourceGroupIndex;
import com.ravenherz.cse.util.video.VideoTranscodeService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrphanSweepTest {

    @Test
    void deletesChunksNoLiveResourceAppOrThemeStillLists() {
        ObjectId groupId = new ObjectId();
        ObjectId keptResourceId = new ObjectId();
        ObjectId keptChunk = new ObjectId();
        ObjectId previewChunk = new ObjectId();
        ObjectId appChunk = new ObjectId();
        ObjectId themeChunk = new ObjectId();
        ObjectId looseChunk = new ObjectId();

        ResourceGroupEntity group = new ResourceGroupEntity();
        group.setId(EntityId.of(groupId.toHexString()));

        ResourceEntity kept = new ResourceEntity();
        kept.setId(EntityId.of(keptResourceId.toHexString()));
        ResourceData file = new ResourceData();
        file.addDataChunkId(keptChunk);
        kept.setResourceData(file);
        ResourceData preview = new ResourceData();
        preview.addDataChunkId(previewChunk);
        kept.setPreviewData(preview);

        AppEntity app = new AppEntity();
        AppData appData = new AppData();
        appData.addDataChunkId(appChunk);
        app.setAppData(appData);

        ThemeEntity theme = new ThemeEntity();
        ThemeData themeData = new ThemeData();
        themeData.addDataChunkId(themeChunk);
        theme.setThemeData(themeData);

        ServiceProvider services = mock(ServiceProvider.class);
        ResourceGroupService groups = mock(ResourceGroupService.class);
        ResourceService resources = mock(ResourceService.class);
        ItemService items = mock(ItemService.class);
        PlaylistService playlists = mock(PlaylistService.class);
        AppService apps = mock(AppService.class);
        ThemeService themes = mock(ThemeService.class);
        VideoTranscodeService transcodes = mock(VideoTranscodeService.class);
        when(services.getResourceGroupService()).thenReturn(groups);
        when(services.getResourceService()).thenReturn(resources);
        when(services.getItemService()).thenReturn(items);
        when(services.getPlaylistService()).thenReturn(playlists);
        when(services.getAppService()).thenReturn(apps);
        when(services.getThemeService()).thenReturn(themes);
        when(services.getVideoTranscodeService()).thenReturn(transcodes);
        when(groups.getAllGroups()).thenReturn(List.of(group));
        when(resources.listSizeHints()).thenReturn(List.of(
                new ResourceSizeHint(keptResourceId, groupId, "/files/a.jpg", 10),
                new ResourceSizeHint(new ObjectId(), new ObjectId(), "/files/gone.jpg", 10)));
        when(resources.getAll()).thenReturn(List.of(kept));
        when(items.getAll()).thenReturn(List.of());
        when(playlists.getAllPlaylists()).thenReturn(List.of());
        when(apps.getAllApps()).thenReturn(List.of(app));
        when(themes.getAllThemes()).thenReturn(List.of(theme));
        when(transcodes.getAll()).thenReturn(List.of());
        when(resources.listDataChunkIds()).thenReturn(List.of(
                keptChunk, previewChunk, appChunk, themeChunk, looseChunk));

        String report = new OrphanSweep(services, mock(ResourceGroupIndex.class),
                mock(ContentProtectedAndCacheController.class)).deleteOrphans();

        assertTrue(report.contains("chunks 1"), report);
        verify(resources).deleteByPublicPath("/files/gone.jpg");
        verify(resources).deleteDataChunk(looseChunk);
        verify(resources, never()).deleteDataChunk(keptChunk);
        verify(resources, never()).deleteDataChunk(previewChunk);
        verify(resources, never()).deleteDataChunk(appChunk);
        verify(resources, never()).deleteDataChunk(themeChunk);
    }
}
