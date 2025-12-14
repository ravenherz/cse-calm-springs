package com.ravenherz.rhzwe.dal.dto.basic;

import com.ravenherz.rhzwe.dal.dto.basic.enums.SecurityLevel;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Indexed;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

@Entity
public final class AccountData {

    @Entity
    public static class AccountSession {
        String remoteAddress;
        String userAgent;
        String sessionToken;
        LocalDateTime startedServerDateTime;
        LocalDateTime finishedServerDateTime;
        Boolean finished;

        public AccountSession() {
        }



        public AccountSession(String remoteAddress, String userAgent, String sessionToken,
                LocalDateTime startedServerDateTime) {
            this.remoteAddress = remoteAddress;
            this.userAgent = userAgent;
            this.sessionToken = sessionToken;
            this.startedServerDateTime = startedServerDateTime;
            finished = false;
        }

        public void setRemoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public void setSessionToken(String sessionToken) {
            this.sessionToken = sessionToken;
        }

        public void setStartedServerDateTime(LocalDateTime startedServerDateTime) {
            this.startedServerDateTime = startedServerDateTime;
        }

        public String getRemoteAddress() {
            return remoteAddress;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public String getSessionToken() {
            return sessionToken;
        }

        public LocalDateTime getStartedServerDateTime() {
            return startedServerDateTime;
        }

        public LocalDateTime getFinishedServerDateTime() {
            return finishedServerDateTime;
        }

        public Boolean getFinished() {
            return finished;
        }

        public void setFinishedServerDateTime(LocalDateTime finishedServerDateTime) {
            this.finishedServerDateTime = finishedServerDateTime;
        }

        public void setFinished(Boolean finished) {
            this.finished = finished;
        }

        public boolean checkFakeEquality(AccountSession accountSession) {
            return Objects.equals(remoteAddress, accountSession.remoteAddress) &&
                    Objects.equals(userAgent, accountSession.userAgent) &&
                    Objects.equals(sessionToken, accountSession.sessionToken);
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AccountSession that = (AccountSession) o;
            return Objects.equals(remoteAddress, that.remoteAddress) &&
                    Objects.equals(userAgent, that.userAgent) &&
                    Objects.equals(sessionToken, that.sessionToken) &&
                    Objects.equals(startedServerDateTime, that.startedServerDateTime) &&
                    Objects.equals(finishedServerDateTime, that.finishedServerDateTime) &&
                    Objects.equals(finished, that.finished);
        }

        @Override public int hashCode() {

            return Objects.hash(remoteAddress, userAgent, sessionToken, startedServerDateTime,
                    finishedServerDateTime, finished);
        }
    }

    @Indexed
    private String login;
    private String hash;
    @Indexed
    private String emailAddress;
    private String bio;
    private SecurityLevel level;
    private HashMap<String, String> contacts;
    private HashMap<String, String> extensibleData;
    private boolean loginable = true;
    private Set<AccountSession> sessions;
    private String activationToken;
    public AccountData() {

    }

    public AccountData(String login, String hash,
            String emailAddress, String activationToken) {
        this.login = login;
        this.hash = hash;
        this.emailAddress = emailAddress;
        this.level = SecurityLevel.INACTIVE_USER;
        this.loginable = false;
        this.activationToken = activationToken;
    }

    public AccountData(String login, String hash,
            String emailAddress, SecurityLevel level) {
        this.login = login;
        this.hash = hash;
        this.emailAddress = emailAddress;
        this.level = level;
        loginable = !level.equals(SecurityLevel.GUIDE);
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public SecurityLevel getLevel() {
        return level;
    }

    public void setLevel(SecurityLevel level) {
        this.level = level;
    }

    public HashMap<String, String> getContacts() {
        return contacts;
    }

    public void setContacts(HashMap<String, String> contacts) {
        this.contacts = contacts;
    }

    public HashMap<String, String> getExtensibleData() {
        return extensibleData;
    }

    public void setExtensibleData(HashMap<String, String> extensibleData) {
        this.extensibleData = extensibleData;
    }

    public boolean isLoginable() {
        return loginable;
    }

    public void setLoginable(boolean loginable) {
        this.loginable = loginable;
    }

    public Set<AccountSession> getSessions() {
        return sessions;
    }

    public void setSessions(Set<AccountSession> sessions) {
        this.sessions = sessions;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccountData that = (AccountData) o;
        return loginable == that.loginable &&
                Objects.equals(login, that.login) &&
                Objects.equals(hash, that.hash) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(bio, that.bio) &&
                level == that.level &&
                Objects.equals(contacts, that.contacts) &&
                Objects.equals(extensibleData, that.extensibleData) &&
                Objects.equals(sessions, that.sessions);
    }

    @Override public int hashCode() {

        return Objects
                .hash(login, hash, emailAddress, bio, level, contacts, extensibleData, loginable,
                        sessions);
    }

    @Override public String toString() {
        return "AccountData{" +
                "login='" + login + '\'' +
                ", hash='" + hash + '\'' +
                ", emailAddress='" + emailAddress + '\'' +
                ", bio='" + bio + '\'' +
                ", level=" + level +
                ", contacts=" + contacts +
                ", extensibleData=" + extensibleData +
                ", loginable=" + loginable +
                ", sessions=" + sessions +
                '}';
    }

    public boolean activate(String activationToken) {
        if (SecurityLevel.INACTIVE_USER.equals(this.level)) {
            if (this.activationToken.equals(activationToken)) {
                loginable = true;
                this.activationToken = null;
                this.level = SecurityLevel.ACTIVE_USER;
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
