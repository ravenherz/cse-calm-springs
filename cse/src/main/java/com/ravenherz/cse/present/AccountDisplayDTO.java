package com.ravenherz.cse.present;

import java.time.LocalDateTime;
import java.util.List;

public class AccountDisplayDTO {
    private String id;
    private String login;
    private String shownName;
    private String avatar;
    private String initial;
    private String emailAddress;
    private String level;
    private String roleId;
    private String roleName;
    private boolean owner;
    private boolean self;
    private boolean canEdit;
    private String lockHint;
    private List<RoleOptionDTO> assignableRoles;
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

    public String getShownName() { return shownName; }
    public void setShownName(String shownName) { this.shownName = shownName; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getInitial() { return initial; }
    public void setInitial(String initial) { this.initial = initial; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public boolean isOwner() { return owner; }
    public void setOwner(boolean owner) { this.owner = owner; }

    public boolean isSelf() { return self; }
    public void setSelf(boolean self) { this.self = self; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public String getLockHint() { return lockHint; }
    public void setLockHint(String lockHint) { this.lockHint = lockHint; }

    public List<RoleOptionDTO> getAssignableRoles() { return assignableRoles; }
    public void setAssignableRoles(List<RoleOptionDTO> assignableRoles) { this.assignableRoles = assignableRoles; }

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