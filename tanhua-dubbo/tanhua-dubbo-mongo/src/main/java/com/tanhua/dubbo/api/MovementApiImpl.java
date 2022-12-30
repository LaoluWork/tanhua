package com.tanhua.dubbo.api;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.dubbo.utils.IdWorker;
import com.tanhua.dubbo.utils.TimeLineService;
import com.tanhua.model.mongo.Movement;
import com.tanhua.model.mongo.MovementTimeLine;
import com.tanhua.model.vo.PageResult;
import org.apache.dubbo.config.annotation.DubboService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-01 21:24
 */
@DubboService
public class MovementApiImpl implements MovementApi{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private TimeLineService timeLineService;

    //发布动态
    @Override
    public String publish(Movement movement) {
        try {
            //1.保存动态详情
            //设置Pid
            movement.setPid(idWorker.getNextId("movement"));
            //设置时间
            movement.setCreated(System.currentTimeMillis());

            mongoTemplate.save(movement);

            //2.保存对应movementTimeLine表
            timeLineService.saveTimeLine(movement.getUserId(), movement.getId());

        } catch (Exception e) {
            //忽略事务处理
            e.printStackTrace();
        }
        return movement.getId().toHexString();
    }

    @Override
    public PageResult findByUserId(Long userId, Integer page, Integer pagesize) {
        Criteria criteria = Criteria.where("userId").is(userId).and("state").is(1);
        Query query = Query.query(criteria).skip((page - 1) * pagesize).limit(pagesize)
                .with(Sort.by(Sort.Order.desc("created")));
        List<Movement> movements = mongoTemplate.find(query, Movement.class);
        return new PageResult(page, pagesize, 0L, movements);
    }

    /**
     *
     * @param friendId  当前操作用户的id
     * @return
     */
    @Override
    public List<Movement> findFriendMovements(Integer page, Integer pagesize, Long friendId) {
        //1.先根据FriendId去TimeLiness表查出数据
        Query query = Query.query(Criteria.where("friendId").is(friendId)).limit(pagesize).skip((page - 1) * pagesize)
                .with(Sort.by(Sort.Order.desc("created")));
        List<MovementTimeLine> movements = mongoTemplate.find(query, MovementTimeLine.class);
        //2.提取出动态id列表
        List<ObjectId> ids = CollUtil.getFieldValues(movements, "movementId", ObjectId.class);
        //3.根据动态id列表查询出动态详情
        Query movementQuery = Query.query(Criteria.where("id").in(ids).and("state").is(1));
        return mongoTemplate.find(movementQuery, Movement.class);
    }

    //根据pid数组查询动态
    @Override
    public List<Movement> findMovementsByPids(List<Long> pids) {
        Query query = Query.query(Criteria.where("pid").in(pids));
        return mongoTemplate.find(query, Movement.class);
    }

    //随机获取多条动态数据
    @Override
    public List<Movement> randomMovements(Integer counts) {
        //1.创建统计对象， 设置统计参数
        TypedAggregation<Movement> aggregation = Aggregation.newAggregation(Movement.class, Aggregation.sample(counts));
        //2.调用mongoTemplate方法统计
        AggregationResults<Movement> results = mongoTemplate.aggregate(aggregation, Movement.class);
        //3.获取结果
        return results.getMappedResults();
    }


    @Override
    public Movement findById(String movementId) {
        return mongoTemplate.findById(movementId, Movement.class);
    }

    @Override
    public PageResult findByUserId(Long uid, Integer state, Integer page, Integer pagesize) {
        //因为uid和state可能是空，所以要判断
        Query query = new Query();
        if(uid != null) {
            query.addCriteria(Criteria.where("userId").is(uid));
        }
        if(state != null) {
            query.addCriteria(Criteria.where("state").is(state));
        }
        long count = mongoTemplate.count(query, Movement.class);
        query.limit(pagesize).skip((page - 1) * pagesize)
                .with(Sort.by(Sort.Order.desc("created")));
        List<Movement> movements = mongoTemplate.find(query, Movement.class);
        return new PageResult(page, pagesize, count, movements);
    }

    //修改动态的state
    @Override
    public void update(String movementId, int state) {
        Query query = Query.query(Criteria.where("id").is(movementId));
        Update update = Update.update("state", state);
        mongoTemplate.updateFirst(query, update, Movement.class);
    }


}
