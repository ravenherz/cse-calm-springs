package com.ravenherz.cse.dal.dao;

import com.ravenherz.cse.dal.dto.RoleEntity;

import java.util.List;

public interface RoleService {

    List<RoleEntity> getAll();

    RoleEntity getById(String idHex);

    RoleEntity getBySlug(String slug);

    RoleEntity guest();

    RoleEntity owner();

    RoleEntity insert(RoleEntity role);

    boolean replace(RoleEntity role);

    boolean delete(RoleEntity role);

    void ensureSeeded();

    void ensureIndexes();

    void invalidateCache();
}
