package com.ravenherz.cse.controller;

import com.ravenherz.cse.controller.objects.RestResponse;
import com.ravenherz.cse.controller.publicsite.PublicSiteApi;
import com.ravenherz.cse.util.AuthRateLimiter;
import com.ravenherz.cse.util.Json;
import com.ravenherz.cse.util.PasswordHashes;
import com.ravenherz.cse.dal.dto.AccountEntity;
import com.ravenherz.cse.dal.dto.basic.AccountData;
import com.ravenherz.cse.dal.dto.basic.enums.SecurityLevel;
import com.ravenherz.cse.util.MarkdownRenderer;
import com.ravenherz.cse.util.PlaylistEmbedProcessor;
import com.ravenherz.cse.util.helpers.HttpErrorHelper;
import com.ravenherz.cse.util.helpers.HttpErrorHelper.HttpErrorDescription;

import com.ravenherz.cse.util.html.impl.TagForm;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@DependsOn("fileUtils")
public class JsonApiController extends AbstractController {

    public enum Param {
        ACCOUNT_LOGIN("ACCOUNT_LOGIN", true),
        ACCOUNT_PASSWORD("ACCOUNT_PASSWORD", true),
        ACCOUNT_PASSWORD_RETYPE("ACCOUNT_PASSWORD_RETYPE", true),
        ACCOUNT_EMAIL("ACCOUNT_EMAIL", true),
        ACCOUNT_SHOWN_NAME("ACCOUNT_SHOWN_NAME", true);

        Param(String key) {
            required = false;
            this.key = key;
        }

        Param(String key, Boolean required) {
            this.required = required;
            this.key = key;
        }

        final String key;
        final Boolean required;

        public String getKey() {
            return key;
        }

        public Boolean getRequired() {
            return required;
        }
    }

    private static final Logger LOGGER = LogManager.getLogger(JsonApiController.class);

    private HttpErrorHelper httpErrorHelper;
    private PasswordHashes passwordHashes;
    private AuthRateLimiter authRateLimiter;
    private PublicSiteApi publicSiteApi;

    @Autowired @Lazy
    public void setHttpErrorHelper(HttpErrorHelper httpErrorHelper) {
        this.httpErrorHelper = httpErrorHelper;
    }

    @Autowired
    public void setPasswordHashes(PasswordHashes passwordHashes) {
        this.passwordHashes = passwordHashes;
    }

    @Autowired
    public void setAuthRateLimiter(AuthRateLimiter authRateLimiter) {
        this.authRateLimiter = authRateLimiter;
    }

    @Autowired
    public void setPublicSiteApi(PublicSiteApi publicSiteApi) {
        this.publicSiteApi = publicSiteApi;
    }

    private Map<String, String> getMapOfJsonBody(String jsonBody) {
        Map<String, String> jsonMap = new HashMap<>();
        try {
            jsonMap = Json.stringMap(jsonBody);
        } catch (IOException ex) {
            LOGGER.error(ex.getMessage() + " " + jsonBody);
        }
        return jsonMap;
    }

    @RequestMapping(value = "/rest/site", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> site(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "page", required = false) String page,
            @RequestParam(value = "album", required = false) String album,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "error", required = false) String error) throws IOException {
        return publicSiteApi.get(request, response, page, album, tag, category, error);
    }

    @RequestMapping(value = "/rest/forms/render", method = RequestMethod.POST)
    public @ResponseBody RestResponse renderForm(@RequestBody String body) {
        Map<String, String> json = getMapOfJsonBody(body);
        String responseObject = new TagForm().renderTag(json.getOrDefault("form","")).getCode();
        return new RestResponse(200, responseObject, null);
    }

    @RequestMapping(value = "/rest/markdown/render", method = RequestMethod.POST)
    public @ResponseBody RestResponse renderMarkdown(@RequestBody String body) {
        Map<String, String> json = getMapOfJsonBody(body);
        String markdown = json.getOrDefault("markdown", "");
        String html = PlaylistEmbedProcessor.expand(MarkdownRenderer.render(markdown));
        return new RestResponse(200, html, null);
    }

    @RequestMapping(value = "/rest/error", method = RequestMethod.POST)
    public @ResponseBody HttpErrorDescription getErrorData(@RequestBody String body) {
        String code = getMapOfJsonBody(body).getOrDefault("error", "666");
        return httpErrorHelper.getHttpErrorDescByCode(Integer.parseInt(code));
    }

    @RequestMapping(value = "/account/logout", method = RequestMethod.POST)
    public @ResponseBody RestResponse accountLogout(HttpServletRequest request,
            HttpServletResponse response) {
        AccountEntity accountEntity = getAccessor(request, response);
        if (accountEntity != null && request.getCookies() != null) {
            String session = getCookieByKey(request, "auth-session");
            if (session != null && accountEntity.getAccountData().getSessions() != null) {
                accountEntity.getAccountData().getSessions()
                        .forEach(accountSession -> {
                            if (session.equals(accountSession.getSessionToken())) {
                                accountSession.setFinished(true);
                                accountSession.setFinishedServerDateTime(LocalDateTime.now());
                            }
                        });
                serviceProvider.getAccountService().replace(accountEntity);
            }
        }
        deleteAuthCookies(request, response);
        return new RestResponse(200);
    }

    @RequestMapping(value = "/account/activate", method = RequestMethod.GET)
    public void accountActivate (HttpServletRequest request,
            HttpServletResponse response, @RequestParam String login, @RequestParam String token)
            throws IOException {
        AccountEntity accountEntity = serviceProvider.getAccountService().getByLogin(login);
        if (accountEntity != null) {
            if (accountEntity.getAccountData().activate(token)) {
                serviceProvider.getAccountService().replace(accountEntity);
                error(200, request, response);
            }
        } else {
            error(400, request, response);
        }
    }

    @RequestMapping(value = "/account/register", method = RequestMethod.POST)
    public @ResponseBody RestResponse accountRegister(HttpServletRequest request,
            HttpServletResponse response, @RequestBody String body) {
        Map<String, String> json = getMapOfJsonBody(body);
        String login = json.getOrDefault(Param.ACCOUNT_LOGIN.name(),"");
        String password = json.getOrDefault(Param.ACCOUNT_PASSWORD.name(), "");
        String passwordRetype = json.getOrDefault(Param.ACCOUNT_PASSWORD_RETYPE.name(),"");
        String email = json.getOrDefault(Param.ACCOUNT_EMAIL.name(),"");
        if (login.isBlank() || email.isBlank()) {
            return new RestResponse(400, null, "Login and email are required");
        }
        if (serviceProvider.getAccountService().getByLogin(login) != null) {
            return new RestResponse(400, null, "Login is already taken");
        }
        if (serviceProvider.getAccountService().getByEmail(email) != null) {
            return new RestResponse(400, null, "Email is already taken");
        }
        if (password.length() == 0 || passwordRetype.length() == 0 || !password.equals(passwordRetype)) {
            return new RestResponse(400, null, "Passwords don't match");
        }
        if (passwordHashes.isTooLong(password)) {
            return new RestResponse(400, null, "Password is too long");
        }

        AccountEntity accountEntity = new AccountEntity(
                new AccountData(login, passwordHashes.hash(password),
                        email, SecurityLevel.ACTIVE_USER));
        serviceProvider.getAccountService().insert(accountEntity);
        AccountEntity stored = serviceProvider.getAccountService().getByLogin(login);
        if (stored != null) {
            issueSession(stored, request, response);
        }
        return new RestResponse(200, null, "all done");
    }

    @RequestMapping(value = "/account/auth", method = RequestMethod.POST)
    public @ResponseBody RestResponse accountAuth(HttpServletRequest request,
            HttpServletResponse response, @RequestBody String body) {
        String ip = clientIp(request);
        if (!authRateLimiter.allow(ip)) {
            return new RestResponse(429, null, "Too many login attempts. Try again later.");
        }
        Map<String, String> json = getMapOfJsonBody(body);
        String login = json.getOrDefault("loginpanel-username", "").trim();
        String password = json.getOrDefault("loginpanel-password", "").trim();
        if (login.length() == 0 || password.length() == 0) {
            authRateLimiter.recordFailure(ip);
            return new RestResponse(400, null, "Missing credentials");
        }
        if (passwordHashes.isTooLong(password)) {
            authRateLimiter.recordFailure(ip);
            return new RestResponse(400, null, "Bad credentials");
        }
        AccountEntity entity = serviceProvider.getAccountService().getByLogin(login);
        if (entity == null || entity.getAccountData() == null) {
            authRateLimiter.recordFailure(ip);
            return new RestResponse(400, null, "Bad credentials");
        }
        if (!entity.getAccountData().isLoginable()) {
            authRateLimiter.recordFailure(ip);
            return new RestResponse(400, null, "You are not allowed to login");
        }
        String stored = entity.getAccountData().getHash();
        boolean legacyClientHash = PasswordHashes.isUnmigratedClientHash(password, stored);
        if (!legacyClientHash && !passwordHashes.verify(password, stored)) {
            authRateLimiter.recordFailure(ip);
            return new RestResponse(400, null, "Bad credentials");
        }
        if (!legacyClientHash && passwordHashes.needsRehash(stored)) {
            entity.getAccountData().setHash(passwordHashes.hash(password));
        }
        if (!issueSession(entity, request, response)) {
            return new RestResponse(500, null, "Server error");
        }
        authRateLimiter.recordSuccess(ip);
        return new RestResponse(200);
    }
}
