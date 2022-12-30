package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.RecommendUser;
import com.tanhua.model.vo.PageResult;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-30 20:09
 */
public interface RecommendUserApi {

    //查询今日佳人
    RecommendUser queryWithMaxScore(Long toUserId);

    //分页查询推荐用户列表
    PageResult queryRecommendUserList(Integer page, Integer pagesize, Long toUserId);

    //根据两者id来查询推荐数据
    RecommendUser queryByUserId(Long userId, Long userId1);

    //探花-推荐用户列表(卡片）
    List<RecommendUser> QueryCardsList(Long userId, int i);
}
