package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.UserLike;

import java.util.List;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-05 21:21
 */
public interface UserLikeApi {
    // 保存或更新
    Boolean saveOrUpdate(Long userId, Long likeUserId, boolean isLike);

    //查询访客的详情列表
    Map<Long, UserLike> queryByLikeUserIds(Long userId, List<Long> ids);
}
