package com.ravenherz.cse.dal.dto;

import com.ravenherz.cse.constants.MongoCollections;
import com.ravenherz.cse.dal.dto.basic.AppAccountGrant;
import com.ravenherz.cse.dal.dto.basic.RoleGrant;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = MongoCollections.DATABASE_ROLE_MATRIX)
public final class RoleMatrixDocument {

    public static final ObjectId SINGLETON_ID = new ObjectId("c5e000000000000000000001");

    @Id
    private ObjectId id = SINGLETON_ID;
    private List<RoleGrant> grants = new ArrayList<>();
    private List<AppAccountGrant> appGrants = new ArrayList<>();

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<RoleGrant> getGrants() {
        if (grants == null) {
            grants = new ArrayList<>();
        }
        return grants;
    }

    public void setGrants(List<RoleGrant> grants) {
        this.grants = grants == null ? new ArrayList<>() : grants;
    }

    public List<AppAccountGrant> getAppGrants() {
        if (appGrants == null) {
            appGrants = new ArrayList<>();
        }
        return appGrants;
    }

    public void setAppGrants(List<AppAccountGrant> appGrants) {
        this.appGrants = appGrants == null ? new ArrayList<>() : appGrants;
    }
}
