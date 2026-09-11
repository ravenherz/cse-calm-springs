package com.ravenherz.cse.present;

public class RoleOptionDTO {
    private String id;
    private String name;
    private boolean loginable;

    public RoleOptionDTO() {
    }

    public RoleOptionDTO(String id, String name, boolean loginable) {
        this.id = id;
        this.name = name;
        this.loginable = loginable;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isLoginable() { return loginable; }
    public void setLoginable(boolean loginable) { this.loginable = loginable; }
}
