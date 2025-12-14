package com.ravenherz.rhzwe.controller;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.ravenherz.rhzwe.controller.objects.RestResponse;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.basic.AccountData;
import com.ravenherz.rhzwe.dal.dto.basic.AccountData.AccountSession;
import com.ravenherz.rhzwe.util.StringUtils;
import com.ravenherz.rhzwe.util.helpers.HttpErrorHelper;
import com.ravenherz.rhzwe.util.helpers.HttpErrorHelper.HttpErrorDescription;

import com.ravenherz.rhzwe.util.html.impl.TagForm;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@DependsOn("fileUtils")
public class RestController extends AbstractController {

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

    private static final Logger LOGGER = LogManager.getLogger(RestController.class);

    private HttpErrorHelper httpErrorHelper;

    @Autowired @Lazy
    public void setHttpErrorHelper(HttpErrorHelper httpErrorHelper) {
        this.httpErrorHelper = httpErrorHelper;
    }

    @PostConstruct
    public void postConstruct() {
    }

    @SuppressWarnings(value = "unchecked")
    private Map<String, String> getMapOfJsonBody(String jsonBody) {
        Map<String, String> jsonMap = new HashMap<>();
        try {
            jsonMap = new Gson().fromJson(jsonBody, HashMap.class);
        } catch (JsonSyntaxException ex) {
            LOGGER.error(ex.getMessage() + " " + jsonBody);
        }
        return jsonMap;
    }

    @RequestMapping(value = "/rest/forms/render", method = RequestMethod.POST)
    public @ResponseBody RestResponse renderForm(@RequestBody String body) {
        Map<String, String> json = getMapOfJsonBody(body);
        String responseObject = new TagForm().renderTag(json.getOrDefault("form","")).getCode();
        return new RestResponse(200, responseObject, null);
    }

    @RequestMapping(value = "/rest/error", method = RequestMethod.POST)
    public @ResponseBody HttpErrorDescription getErrorData(@RequestBody String body) {
        String code = getMapOfJsonBody(body).getOrDefault("error", "666");
        return httpErrorHelper.getHttpErrorDescByCode(Integer.parseInt(code));
    }

    @RequestMapping(value = "/account/logout", method = RequestMethod.GET)
    public @ResponseBody RestResponse accountLogout(HttpServletRequest request,
            HttpServletResponse response) {
        AccountEntity accountEntity = getAccessor(request, response);
        if (accountEntity != null && request.getCookies() != null) {
            String login = getCookieByKey(request, "auth-login");
            String session = getCookieByKey(request, "auth-session");
            if (login != null && session != null) {
                deleteAuthCookies(response);
                accountEntity.getAccountData().getSessions()
                        .forEach(accountSession -> {
                            if (accountSession.getSessionToken().equals(session)) {
                                accountSession.setFinished(true);
                                accountSession.setFinishedServerDateTime(LocalDateTime.now());
                            }
                        });
                serviceProvider.getAccountService().replace(accountEntity);
            }
        }
        return new RestResponse(400);
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
        String shownName = json.getOrDefault(Param.ACCOUNT_SHOWN_NAME.name(), "");
        if (serviceProvider.getAccountService().getByLogin(login) != null) {
            return new RestResponse(400, null, "Login is already taken");
        }
        if (serviceProvider.getAccountService().getByEmail(email) != null) {
            return new RestResponse(400, null, "Email is already taken");
        }
        if (password.length() == 0 || passwordRetype.length() == 0 || !password.equals(passwordRetype)) {
            return new RestResponse(400, null, "Passwords don't match");
        }
        if (shownName.equals("")) {
            shownName = null;
        }

        AccountEntity accountEntity = new AccountEntity(
                new AccountData(login, password,
                        email,
                        StringUtils.generateRandomToken().substring(28)));
        serviceProvider.getAccountService().insert(accountEntity);
        return new RestResponse(200, null, "all done");
    }

    @RequestMapping(value = "/account/auth", method = RequestMethod.POST)
    public @ResponseBody RestResponse accountAuth(HttpServletRequest request,
            HttpServletResponse response, @RequestBody String body) {
        Map<String, String> json = getMapOfJsonBody(body);
        String login = json.getOrDefault("loginpanel-username", "").trim();
        String hash = json.getOrDefault("loginpanel-password", "").trim();
        if (login.length() > 0 && hash.length() > 0) {
            AccountEntity entity = serviceProvider.getAccountService().getByLogin(login);
            if (entity != null) {
                if (!entity.getAccountData().isLoginable()) {
                    return new RestResponse(400, null, "You are not allowed to login");
                }

                if (hash.equals(entity.getAccountData().getHash())) {

                    String remoteAddress = request.getRemoteAddr();
                    String userAgent = request.getHeader("User-Agent");
                    String sessionToken = StringUtils.generateRandomToken();

                    AccountSession accountSession = new AccountSession(remoteAddress, userAgent,
                            sessionToken,
                            LocalDateTime.now());

                    if (entity.getAccountData().getSessions() == null) {
                        entity.getAccountData().setSessions(new HashSet<>());
                    }
                    entity.getAccountData().getSessions().add(accountSession);
                    if (serviceProvider.getAccountService().replace(entity)) {

                        addCookies(response, new Cookie("auth-login", login),
                                new Cookie("auth-session", sessionToken));
                        return new RestResponse(200);
                    }
                    return new RestResponse(500, null, "Server error");
                }
            }
            return new RestResponse(400, null, "Bad credentials");
        } else {
            return new RestResponse(400, null, "Missing credentials");
        }
    }
}