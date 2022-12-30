package com.tanhua.dubbo.api;


import com.mongodb.client.result.DeleteResult;
import com.sun.org.apache.xerces.internal.dom.PSVIDOMImplementationImpl;
import com.tanhua.dubbo.utils.IdWorker;
import com.tanhua.model.mongo.FocusUser;
import com.tanhua.model.mongo.Video;
import com.tanhua.model.vo.PageResult;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@DubboService
public class VideoApiImpl implements VideoApi {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdWorker idWorker;

    // 保存视频数据
    @Override
    public String save(Video video) {
        // 1.设置属性
        video.setVid(idWorker.getNextId("video"));
        video.setCreated(System.currentTimeMillis());
        // 2.保存到数据库
        mongoTemplate.save(video);
        // 3.返回video数据的id
        return video.getId().toHexString();
    }

    @Override
    public List<Video> findVideosByVids(List<Long> vids) {
        Query query = Query.query(Criteria.where("vid").in(vids));
        return mongoTemplate.find(query, Video.class);
    }

    @Override
    public List<Video> queryVideoList(int page, Integer pagesize) {
        Query query = new Query().skip((page - 1) * pagesize).limit(pagesize)
                .with(Sort.by(Sort.Order.desc("created")));
        return mongoTemplate.find(query, Video.class);
    }

    //小视频里关注用户
    @Override
    public String focusUser(Long userId, Long followUserId) {
        // 1.构建FocusUser对象
        FocusUser user = new FocusUser();
        user.setUserId(userId);
        user.setFollowUserId(followUserId);
        user.setCreated(System.currentTimeMillis());
        // 2.保存到数据库
        mongoTemplate.save(user);
        return user.getId().toHexString();
    }

    //小视频里取消关注用户
    @Override
    public Long userUnFocus(Long userId, Long unFollowId) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("followUserId").is(unFollowId));
        DeleteResult remove = mongoTemplate.remove(query, FocusUser.class);
        return remove.getDeletedCount();
    }


    // 根据某个用户id分页查询
    @Override
    public PageResult findAllVideos(Integer page, Integer pagesize, Long uid) {
        Query query = Query.query(Criteria.where("userId").is(uid));
        long count = mongoTemplate.count(query, Video.class);
        query.limit(pagesize).skip((page - 1) * pagesize)
                .with(Sort.by(Sort.Order.desc("created")));
        List<Video> videos = mongoTemplate.find(query, Video.class);
        return new PageResult(page, pagesize, count, videos);
    }
}