package com.ravenherz.cse.util.video;

import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.Store;

public interface VideoTranscodeService extends Store {

    VideoTranscodeEntity findByResourceId(EntityId resourceId);

    VideoTranscodeEntity upsert(EntityId resourceId, VideoTranscodeStatus status, int percent, String error);

    VideoTranscodePage page(int page, int size);
}
