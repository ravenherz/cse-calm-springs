package com.ravenherz.cse.admin;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/editor/video")
public class EditorVideoController {

    private final VideoProgressFeed feed;

    public EditorVideoController(VideoProgressFeed feed) {
        this.feed = feed;
    }

    @GetMapping(value = "/progress", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> progress(@RequestParam(value = "ids", required = false) String ids,
            HttpServletRequest request) {
        return ResponseEntity.ok(feed.progress(ids, request));
    }
}
