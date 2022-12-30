package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.Friend;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;


/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-04 22:17
 */
@DubboService
public class FriendApiImpl implements  FriendApi{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void save(Long userId, Long friendId) {
        //1.保存自己的好友数据
        Query query1 = Query.query(Criteria.where("userId").is(userId).and("friendId").is(friendId));
        //如果好友关系在数据库中不存在，那就新建保存
        if (!mongoTemplate.exists(query1, Friend.class)) {
            Friend friend = new Friend();
            friend.setUserId(userId);
            friend.setFriendId(friendId);
            friend.setCreated(System.currentTimeMillis());
            mongoTemplate.save(friend);
        }
        //2.保存好友的自己数据
        Query query2 = Query.query(Criteria.where("userId").is(friendId).and("friendId").is(userId));
        //如果好友关系在数据库中不存在，那就新建保存
        if (!mongoTemplate.exists(query2, Friend.class)) {
            Friend friend = new Friend();
            friend.setUserId(friendId);
            friend.setFriendId(userId);
            friend.setCreated(System.currentTimeMillis());
            mongoTemplate.save(friend);
        }
    }

    @Override
    public List<Friend> findByUserId(Long userId, Integer page, Integer pagesize) {
        Criteria criteria = Criteria.where("userId").is(userId);
        Query query = Query.query(criteria).skip((page - 1) * pagesize).limit(pagesize)
                .with(Sort.by(Sort.Order.desc("created")));

        return mongoTemplate.find(query, Friend.class);

    }
}
