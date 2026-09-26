package com.ravenherz.cse.admin;

import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.security.AccountAccessor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

@Controller
@RequestMapping("/editor/transcode")
public class EditorTranscodeController {

    private final AccountAccessor authSupport;
    private final EditorChrome editorChrome;
    private final TranscodeQueue transcodeQueue;

    public EditorTranscodeController(AccountAccessor authSupport, EditorChrome editorChrome,
            TranscodeQueue transcodeQueue) {
        this.authSupport = authSupport;
        this.editorChrome = editorChrome;
        this.transcodeQueue = transcodeQueue;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        editorChrome.apply(model, accessor);
        model.addAttribute("queue", transcodeQueue.snapshot(request));
        return "/admin/editor-transcode";
    }

    @GetMapping(value = "/queue", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<TranscodeQueue.QueueSnapshot> queue(HttpServletRequest request) {
        return ResponseEntity.ok(transcodeQueue.snapshot(request));
    }
}
