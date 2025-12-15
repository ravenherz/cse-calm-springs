package com.ravenherz.rhzwe.controller;

import com.ravenherz.rhzwe.dal.ServiceProvider;
import com.ravenherz.rhzwe.dal.dto.AccountEntity;
import com.ravenherz.rhzwe.dal.dto.basic.AccountData.AccountSession;
import com.ravenherz.rhzwe.util.Settings;
import com.ravenherz.rhzwe.util.html.ControllerAccessibleTag;
import com.ravenherz.rhzwe.util.html.CustomHtmlTag;
import com.ravenherz.rhzwe.util.io.FileUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@DependsOn(value = {"settings", "serviceProvider", "fileUtils"})
public abstract class AbstractController {

    private static final Logger LOGGER = Logger.getLogger(AbstractController.class.getName());

    @Autowired
    @Lazy
    protected List<? extends CustomHtmlTag> tags;

    @Autowired
    @Lazy
    protected List<ControllerAccessibleTag> controllerAccessibleTags;

    protected static Settings settings;
    protected static FileUtils fileUtils;
    protected static ServiceProvider serviceProvider;

    @Autowired
    public void setSettings(Settings settingsImpl) {
        settings = settingsImpl;
    }

    @Autowired
    public void setFileUtils(FileUtils fileUtilsImpl) {
        fileUtils = fileUtilsImpl;
    }

    @Autowired @Lazy
    public void setServiceProvider(ServiceProvider serviceProviderImpl) {
        serviceProvider = serviceProviderImpl;
    }

    protected AccountEntity getAccessor(HttpServletRequest request,
            HttpServletResponse response) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if ("auth-login".equals(cookie.getName()) && cookie.getValue() != null) {
                AccountEntity accountEntity = serviceProvider.getAccountService()
                        .getByLogin(cookie.getValue());
                if (accountEntity == null) {
                    deleteAuthCookies(response);
                    LOGGER.warning(String.format("Someone (%s) is attempting to get access illegally",
                            request.getRemoteAddr()));
                    return null;
                }
                try {
                    String remoteAddress = request.getRemoteAddr();
                    String userAgent = request.getHeader("User-Agent");
                    String sessionToken = Objects.requireNonNull(Arrays.stream(request.getCookies())
                            .filter(c -> "auth-session".equals(c.getName()))
                            .findFirst().orElse(null)).getValue();
                    AccountSession fakeAccountSession = new AccountSession(remoteAddress, userAgent,
                            sessionToken, null);
                    if (accountEntity.getAccountData().getSessions().stream()
                            .filter(accountSession ->
                                    accountSession.checkFakeEquality(fakeAccountSession))
                            .collect(Collectors.toSet()).size() > 0) {
                        return accountEntity;
                    } else {
                        deleteAuthCookies(response);
                    }
                } catch (NullPointerException ex) {
                    LOGGER.warning(ex.getMessage());
                    return null;
                }
            }
        }
        return null;

    }

    protected String getCookieByKey(HttpServletRequest request, String key) {
        if (request.getCookies() != null && key != null) {
            for (Cookie cookie : request.getCookies()) {
                if (key.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    protected void deleteCookies(HttpServletResponse response,
            String... keys) {
        Cookie[] cookies = new Cookie[keys.length];
        for (int i = 0; i < keys.length; i++) {
            Cookie cookie = new Cookie(keys[i], "");
            cookies[i] = cookie;
        }
        addCookies(response, 0, cookies);
    }

    protected void deleteAuthCookies(HttpServletResponse response) {
        deleteCookies(response, "auth-login", "auth-session");
    }

    protected void addCookies(HttpServletResponse response,
            Cookie... cookies) {
        addCookies(response, Integer.MAX_VALUE, cookies);
    }

    private void addCookies(HttpServletResponse response, int maxAge,
            Cookie... cookies) {
        Arrays.stream(cookies).forEach(cookie -> {
            cookie.setPath("/");
            cookie.setMaxAge(maxAge);
            response.addCookie(cookie);
        });
    }

    protected void error(int code, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + String.format("/?error=%s", code));
    }

}
