package com.ravenherz.rhzwe.present;

import java.time.LocalDateTime;
import java.util.List;

public class AccountDisplayDTO {
    private String id;
    private String login;
    private String emailAddress;
    private String level;
    private boolean loginable;
    private int sessionTotalCount;
    private int sessionActiveCount;
    private LocalDateTime sessionFirstDate;
    private String sessionFirstAddress;
    private String sessionFirstUserAgent;
    private LocalDateTime sessionLastDate;
    private String sessionLastAddress;
    private String sessionLastUserAgent;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public boolean isLoginable() { return loginable; }
    public void setLoginable(boolean loginable) { this.loginable = loginable; }

    public int getSessionTotalCount() { return sessionTotalCount; }
    public void setSessionTotalCount(int sessionTotalCount) { this.sessionTotalCount = sessionTotalCount; }

    public int getSessionActiveCount() { return sessionActiveCount; }
    public void setSessionActiveCount(int sessionActiveCount) { this.sessionActiveCount = sessionActiveCount; }

    public LocalDateTime getSessionFirstDate() { return sessionFirstDate; }
    public void setSessionFirstDate(LocalDateTime sessionFirstDate) { this.sessionFirstDate = sessionFirstDate; }

    public String getSessionFirstAddress() { return sessionFirstAddress; }
    public void setSessionFirstAddress(String sessionFirstAddress) { this.sessionFirstAddress = sessionFirstAddress; }

    public String getSessionFirstUserAgent() { return sessionFirstUserAgent; }
    public void setSessionFirstUserAgent(String sessionFirstUserAgent) { this.sessionFirstUserAgent = sessionFirstUserAgent; }

    public LocalDateTime getSessionLastDate() { return sessionLastDate; }
    public void setSessionLastDate(LocalDateTime sessionLastDate) { this.sessionLastDate = sessionLastDate; }

    public String getSessionLastAddress() { return sessionLastAddress; }
    public void setSessionLastAddress(String sessionLastAddress) { this.sessionLastAddress = sessionLastAddress; }

    public String getSessionLastUserAgent() { return sessionLastUserAgent; }
    public void setSessionLastUserAgent(String sessionLastUserAgent) { this.sessionLastUserAgent = sessionLastUserAgent; }
}