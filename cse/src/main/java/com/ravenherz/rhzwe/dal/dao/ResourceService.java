package com.ravenherz.rhzwe.dal.dao;

import com.ravenherz.rhzwe.dal.dto.ResourceEntity;

public interface ResourceService extends Service {

    ResourceEntity getByPublicPath(String publicPath);
    ResourceEntity getByProtectedPath(String protectedPath);
    void deleteByPublicPath(String publicPath);
}
