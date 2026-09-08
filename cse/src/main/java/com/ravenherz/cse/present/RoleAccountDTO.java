package com.ravenherz.cse.present;

import java.util.List;

public class RoleAccountDTO {
    private String id;
    private String login;
    private String emailAddress;
    private String level;
    private boolean loginable;
    private boolean self;
    private boolean canEdit;
    private String lockHint;
    private List<String> assignableLevels;

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

    public boolean isSelf() { return self; }
    public void setSelf(boolean self) { this.self = self; }

    public boolean isCanEdit() { return canEdit; }
    public void setCanEdit(boolean canEdit) { this.canEdit = canEdit; }

    public String getLockHint() { return lockHint; }
    public void setLockHint(String lockHint) { this.lockHint = lockHint; }

    public List<String> getAssignableLevels() { return assignableLevels; }
    public void setAssignableLevels(List<String> assignableLevels) { this.assignableLevels = assignableLevels; }
}
