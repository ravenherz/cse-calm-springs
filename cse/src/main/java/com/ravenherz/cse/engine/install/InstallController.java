package com.ravenherz.cse.engine.install;

import com.ravenherz.cse.install.InstallException;
import com.ravenherz.cse.install.InstallService;
import com.ravenherz.cse.install.SiteReady;

import com.ravenherz.cse.controller.AuthSupport;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.util.AuthRateLimiter;
import com.ravenherz.cse.engine.util.Json;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/install")
public class InstallController {

    private final SiteReady siteReady;
    private final InstallService installService;
    private final AuthRateLimiter authRateLimiter;
    private final AuthSupport authSupport;

    public InstallController(SiteReady siteReady, InstallService installService,
            AuthRateLimiter authRateLimiter, AuthSupport authSupport) {
        this.siteReady = siteReady;
        this.installService = installService;
        this.authRateLimiter = authRateLimiter;
        this.authSupport = authSupport;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> status() {
        if (siteReady.isConfigured()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(installService.status());
    }

    @PostMapping(value = "/mongo", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mongo(@RequestBody String body, HttpServletRequest request) {
        return guarded(request, () -> installService.connectMongo(parse(body)));
    }

    @PostMapping(value = "/owner", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> owner(@RequestBody String body, HttpServletRequest request,
            HttpServletResponse response) {
        return guarded(request, () -> {
            AccountEntity owner = installService.createOwner(parse(body));
            if (!authSupport.issueSession(owner, request, response)) {
                throw new InstallException(500, "Could not start a session");
            }
            return Map.of("ok", true);
        });
    }

    @PostMapping(value = "/finish", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> finish(@RequestBody String body, HttpServletRequest request) {
        return guarded(request, () -> installService.finish(parse(body),
                request.getContextPath() == null ? "" : request.getContextPath()));
    }

    private ResponseEntity<?> guarded(HttpServletRequest request, Action action) {
        if (siteReady.isConfigured()) {
            return ResponseEntity.notFound().build();
        }
        String key = "install:" + authSupport.clientIp(request);
        if (!authRateLimiter.allow(key)) {
            return ResponseEntity.status(429).body(Map.of("ok", false, "message",
                    "Too many attempts. Try again later."));
        }
        try {
            Map<String, Object> result = action.run();
            authRateLimiter.recordSuccess(key);
            return ResponseEntity.ok(result);
        } catch (InstallException ex) {
            authRateLimiter.recordFailure(key);
            return ResponseEntity.status(ex.getStatus())
                    .body(Map.of("ok", false, "message", ex.getMessage()));
        }
    }

    private static Map<String, String> parse(String body) {
        try {
            return Json.stringMap(body);
        } catch (IOException ex) {
            throw new InstallException(400, "Invalid JSON");
        }
    }

    @FunctionalInterface
    private interface Action {
        Map<String, Object> run();
    }
}
