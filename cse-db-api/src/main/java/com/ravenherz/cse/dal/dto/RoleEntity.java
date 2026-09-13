package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.role.RoleSeeds;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Objects;

@Document(collection = MongoCollections.DATABASE_ROLES)
public final class RoleEntity {

    @Id
    private ObjectId id;
    private String entityVersion = "0.1.0";
    @Indexed(unique = true)
    private String slug;
    private String name;
    private String system;
    private boolean loginable;
    private int sortOrder;
    private boolean archived;

    public RoleEntity() {
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String idHex() {
        return id == null ? null : id.toHexString();
    }

    public String getIdHex() {
        return idHex();
    }

    public String getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(String entityVersion) {
        this.entityVersion = entityVersion;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public boolean isLoginable() {
        return loginable;
    }

    public void setLoginable(boolean loginable) {
        this.loginable = loginable;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public boolean isGuest() {
        return RoleSeeds.SYSTEM_GUEST.equals(system) || RoleSeeds.GUEST.equals(slug);
    }

    public boolean isOwner() {
        return RoleSeeds.SYSTEM_OWNER.equals(system) || RoleSeeds.OWNER.equals(slug);
    }

    public boolean isSystem() {
        return system != null && !system.isBlank();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RoleEntity that)) {
            return false;
        }
        return loginable == that.loginable
                && sortOrder == that.sortOrder
                && archived == that.archived
                && Objects.equals(id, that.id)
                && Objects.equals(slug, that.slug)
                && Objects.equals(name, that.name)
                && Objects.equals(system, that.system);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, slug, name, system, loginable, sortOrder, archived);
    }
}
