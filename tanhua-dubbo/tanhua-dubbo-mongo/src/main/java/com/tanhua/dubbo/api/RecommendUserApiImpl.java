package com.tanhua.dubbo.api;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.model.mongo.RecommendUser;
import com.tanhua.model.mongo.UserLike;
import com.tanhua.model.vo.PageResult;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-30 20:10
 */
@DubboService
public class RecommendUserApiImpl implements RecommendUserApi{

    @Autowired
    private MongoTemplate mongoTemplate;

    //查询今日佳人
    @Override
    public RecommendUser queryWithMaxScore(Long toUserId) {
        //根据toUserId查询，根据频分score排序，取第一条

        //构建Criteria
        Criteria criteria = new Criteria().where("toUserId").is(toUserId);
        //构建Query对象
        Query query = new Query(criteria).with(Sort.by(Sort.Order.desc("score"))).limit(1);
        //调用mongoTemplate查询
        return mongoTemplate.findOne(query, RecommendUser.class);
    }

    // 分页查询推荐好友
    @Override
    public PageResult queryRecommendUserList(Integer page, Integer pagesize, Long toUserId) {
        //1.构建Criteria对象
        Criteria criteria = Criteria.where("toUserId").is(toUserId);
        //2.构建query对象
        Query query = Query.query(criteria);
        //3.查询总数
        long count = mongoTemplate.count(query, RecommendUser.class);
        //4.查询对应页列表
        query.with(Sort.by(Sort.Order.desc("score"))).limit(pagesize).skip((page - 1) * pagesize);
        List<RecommendUser> list = mongoTemplate.find(query, RecommendUser.class);
        //5.构造返回值
        return new PageResult(page, pagesize, count, list);
    }

    //根据两者id来查询推荐数据
    @Override
    public RecommendUser queryByUserId(Long userId, Long toUserId) {
        Criteria criteria = Criteria.where("toUserId").is(toUserId).and("userId").is(userId);
        Query query = Query.query(criteria);
        RecommendUser user = mongoTemplate.findOne(query, RecommendUser.class);
        if (user == null) {
            user = new RecommendUser();
            user.setUserId(userId);
            user.setToUserId(toUserId);
            //构建缘分值
            user.setScore(95d);
        }
        return user;
    }

    //探花-推荐用户列表(卡片）
    @Override
    public List<RecommendUser> QueryCardsList(Long userId, int counts) {
        //1.先查询出当前用户喜欢和不喜欢的数据，并提取出对应的id列表
        List<UserLike> likeList = mongoTemplate.find(Query.query(Criteria.where("userId").is(userId)), UserLike.class);
        List<Long> ids = CollUtil.getFieldValues(likeList, "toUserId", Long.class);
        //2.构造查询推荐用户的条件，排除已经喜欢或不喜欢的用户
        Criteria criteria = Criteria.where("toUserId").is(userId).and("userId").nin(ids);
        //3.使用统计函数，随机获取满足要求的推荐的用户列表
        TypedAggregation<RecommendUser> aggregation = TypedAggregation.newAggregation(RecommendUser.class,
                Aggregation.match(criteria), Aggregation.sample(counts));
        AggregationResults<RecommendUser> results = mongoTemplate.aggregate(aggregation, RecommendUser.class);
        //4.返回对应数据
        return results.getMappedResults();
    }
}
