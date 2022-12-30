package com.tanhua.dubbo.api;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.model.mongo.UserLike;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-05 21:21
 */
@DubboService
public class UserLikeApiImpl implements UserLikeApi{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Boolean saveOrUpdate(Long userId, Long likeUserId, boolean isLike) {
        try {
            //1.查询出对应的数据
            Query query = Query.query(Criteria.where("userId").is(userId).and("likeUserId").is(likeUserId));
            UserLike userLike = mongoTemplate.findOne(query, UserLike.class);
            if (userLike == null) {
                //2.判断数据存在，不存在就保存
                userLike = new UserLike();
                userLike.setUserId(userId);
                userLike.setLikeUserId(likeUserId);
                userLike.setIsLike(isLike);
                userLike.setCreated(System.currentTimeMillis());
                userLike.setUpdated(System.currentTimeMillis());
                mongoTemplate.save(userLike);
            }else {
                //3.存在就更新
                Update update = new Update();
                update.set("isLike", isLike).set("updated", System.currentTimeMillis());
                mongoTemplate.updateFirst(query, update, UserLike.class);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<Long, UserLike> queryByLikeUserIds(Long userId, List<Long> ids) {
        Query query = Query.query(Criteria.where("userId").is(userId).and("likeUserId").in(ids));
        List<UserLike> list = mongoTemplate.find(query, UserLike.class);

        if(CollUtil.isEmpty(list)){
            return new HashMap<>();
        }
        Map<Long, UserLike> map = CollUtil.fieldValueMap(list, "likeUserId");
        return map;
    }
}
