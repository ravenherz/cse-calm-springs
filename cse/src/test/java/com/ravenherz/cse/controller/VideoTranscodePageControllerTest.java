package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.security.AccountAccessor;
import com.ravenherz.cse.util.video.VideoTranscodeEntity;
import com.ravenherz.cse.util.video.VideoTranscodePage;
import com.ravenherz.cse.util.video.VideoTranscodeService;
import com.ravenherz.cse.util.video.VideoTranscodeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VideoTranscodePageControllerTest {

    @Test
    void pageAsksForTwentyWhenTheSizeIsNotListed() throws Exception {
        VideoTranscodeService transcodes = mock(VideoTranscodeService.class);
        VideoTranscodeEntity row = new VideoTranscodeEntity();
        row.setResourceId(EntityId.generate());
        row.setStatus(VideoTranscodeStatus.QUEUED);
        when(transcodes.page(2, 20)).thenReturn(new VideoTranscodePage(List.of(row), 21, 2, 20, 21, 0, 0, 0));
        ServiceProvider services = mock(ServiceProvider.class);
        when(services.getVideoTranscodeService()).thenReturn(transcodes);
        AccountAccessor accessor = (request, response) -> account();
        VideoTranscodePageController controller = new VideoTranscodePageController(accessor, services);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("page", "2");
        request.setParameter("size", "15");

        ResponseEntity<VideoTranscodePage> response = controller.page(request, new MockHttpServletResponse(), 2, 15);

        assertEquals(200, response.getStatusCode().value());
        assertEquals(1, response.getBody().items().size());
        verify(transcodes).page(2, 20);
    }

    @Test
    void pageKeepsFiftyAndOneHundred() throws Exception {
        VideoTranscodeService transcodes = mock(VideoTranscodeService.class);
        when(transcodes.page(1, 100)).thenReturn(VideoTranscodePage.empty(1, 100));
        ServiceProvider services = mock(ServiceProvider.class);
        when(services.getVideoTranscodeService()).thenReturn(transcodes);
        VideoTranscodePageController controller = new VideoTranscodePageController(
                (request, response) -> account(), services);

        ResponseEntity<VideoTranscodePage> response = controller.page(
                new MockHttpServletRequest(), new MockHttpServletResponse(), 1, 100);

        assertEquals(100, response.getBody().size());
        verify(transcodes).page(1, 100);
    }

    private static AccountEntity account() {
        AccountData data = new AccountData();
        data.setLogin("ada");
        return new AccountEntity(data);
    }
}
