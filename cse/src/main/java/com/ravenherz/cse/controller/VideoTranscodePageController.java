package com.ravenherz.cse.controller;

import com.ravenherz.cse.dal.ServiceProvider;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.security.AccountAccessor;
import com.ravenherz.cse.util.video.VideoTranscodePage;
import com.ravenherz.cse.util.video.VideoTranscodePageSize;
import com.ravenherz.cse.util.video.VideoTranscodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping("/editor/transcode/items")
public class VideoTranscodePageController {

    private final AccountAccessor authSupport;
    private final VideoTranscodeService transcodes;

    public VideoTranscodePageController(AccountAccessor authSupport, ServiceProvider serviceProvider) {
        this.authSupport = authSupport;
        this.transcodes = serviceProvider == null ? null : serviceProvider.getVideoTranscodeService();
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<VideoTranscodePage> page(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return ResponseEntity.status(401).build();
        }
        int safePage = page < 1 ? 1 : page;
        int safeSize = VideoTranscodePageSize.normalize(size);
        if (transcodes == null) {
            return ResponseEntity.ok(VideoTranscodePage.empty(safePage, safeSize));
        }
        return ResponseEntity.ok(transcodes.page(safePage, safeSize));
    }
}
