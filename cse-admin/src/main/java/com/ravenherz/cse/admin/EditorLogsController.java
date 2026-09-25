package com.ravenherz.cse.admin;

import com.ravenherz.cse.admin.LogTail.Snapshot;
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
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/editor/logs")
public class EditorLogsController {

    private final AccountAccessor authSupport;
    private final LogTail logTail;

    public EditorLogsController(AccountAccessor authSupport, LogTail logTail) {
        this.authSupport = authSupport;
        this.logTail = logTail;
    }

    @GetMapping
    public String page(Model model, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        AccountEntity accessor = authSupport.getAccessor(request, response);
        if (accessor == null) {
            return null;
        }
        Snapshot snapshot = logTail.read();
        model.addAttribute("username", accessor.getAccountData().getLogin());
        putSnapshot(model, snapshot);
        return "/admin/editor-logs";
    }

    @GetMapping(value = "/tail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> tail() {
        return ResponseEntity.ok(toJson(logTail.read()));
    }

    @GetMapping("/download")
    public void download(HttpServletResponse response) throws IOException {
        Snapshot snapshot = logTail.read();
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + logTail.fileName() + "\"");
        if (!snapshot.isFound()) {
            response.getWriter().write(snapshot.getError() != null
                    ? snapshot.getError()
                    : "No log file found.");
            return;
        }
        response.getWriter().write(snapshot.getText());
    }

    private void putSnapshot(Model model, Snapshot snapshot) {
        model.addAttribute("logFound", snapshot.isFound());
        model.addAttribute("logPath", snapshot.getPath());
        model.addAttribute("logText", snapshot.getText());
        model.addAttribute("logTruncated", snapshot.isTruncated());
        model.addAttribute("logFileBytes", snapshot.getFileBytes());
        model.addAttribute("logWindowLabel", logTail.windowLabel());
        model.addAttribute("logTried", snapshot.getTried());
        model.addAttribute("logError", snapshot.getError());
    }

    private static Map<String, Object> toJson(Snapshot snapshot) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("found", snapshot.isFound());
        body.put("path", snapshot.getPath());
        body.put("text", snapshot.getText());
        body.put("truncated", snapshot.isTruncated());
        body.put("bytes", snapshot.getText() == null ? 0 : snapshot.getText().length());
        body.put("fileBytes", snapshot.getFileBytes());
        body.put("tried", snapshot.getTried());
        body.put("error", snapshot.getError());
        return body;
    }
}
