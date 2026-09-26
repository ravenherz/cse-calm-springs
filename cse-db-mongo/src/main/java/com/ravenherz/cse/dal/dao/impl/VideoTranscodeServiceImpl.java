package com.ravenherz.cse.dal.dao.impl;

import com.ravenherz.cse.dal.DataProvider;
import com.ravenherz.cse.dal.EntityId;
import com.ravenherz.cse.dal.dao.BasicService;
import com.ravenherz.cse.dal.dto.BasicEntity;
import com.ravenherz.cse.util.video.VideoTranscodeEntity;
import com.ravenherz.cse.util.video.VideoTranscodePage;
import com.ravenherz.cse.util.video.VideoTranscodePageSize;
import com.ravenherz.cse.util.video.VideoTranscodeRecords;
import com.ravenherz.cse.util.video.VideoTranscodeService;
import com.ravenherz.cse.util.video.VideoTranscodeStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository(value = "videoTranscodeService")
public class VideoTranscodeServiceImpl extends BasicService implements VideoTranscodeService {

    public VideoTranscodeServiceImpl(DataProvider dataProvider) {
        super(dataProvider);
    }

    @Override
    public List<BasicEntity> getAll() {
        return new ArrayList<>(mongo().findAll(VideoTranscodeEntity.class));
    }

    @Override
    public VideoTranscodeEntity findByResourceId(EntityId resourceId) {
        if (resourceId == null) {
            return null;
        }
        return mongo().findOne(Query.query(Criteria.where("resourceId").is(resourceId.toHexString())),
                VideoTranscodeEntity.class);
    }

    @Override
    public VideoTranscodeEntity upsert(EntityId resourceId, VideoTranscodeStatus status, int percent, String error) {
        if (resourceId == null) {
            return null;
        }
        VideoTranscodeEntity row = VideoTranscodeRecords.apply(findByResourceId(resourceId), resourceId, status,
                percent, error);
        if (row.getId() == null) {
            insert(row);
        } else {
            replace(row);
        }
        return row;
    }

    @Override
    public VideoTranscodePage page(int page, int size) {
        int safeSize = VideoTranscodePageSize.normalize(size);
        int safePage = page < 1 ? 1 : page;
        Query query = new Query()
                .with(Sort.by(Sort.Order.asc("rank"), Sort.Order.desc("updatedAt")))
                .skip((long) (safePage - 1) * safeSize)
                .limit(safeSize);
        List<VideoTranscodeEntity> items = mongo().find(query, VideoTranscodeEntity.class);
        long total = mongo().count(new Query(), VideoTranscodeEntity.class);
        return new VideoTranscodePage(items, total, safePage, safeSize,
                count(VideoTranscodeStatus.QUEUED),
                count(VideoTranscodeStatus.IN_PROGRESS),
                count(VideoTranscodeStatus.DONE),
                count(VideoTranscodeStatus.FAILED));
    }

    private long count(VideoTranscodeStatus status) {
        return mongo().count(Query.query(Criteria.where("status").is(status)), VideoTranscodeEntity.class);
    }
}
