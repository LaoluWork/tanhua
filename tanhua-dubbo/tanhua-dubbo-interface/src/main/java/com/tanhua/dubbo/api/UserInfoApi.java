package com.tanhua.dubbo.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tanhua.model.domain.UserInfo;

import java.util.List;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-15 9:49
 */
public interface UserInfoApi {

    void save(UserInfo userInfo);

    void update(UserInfo userInfo);

    UserInfo findById(Long userID);

    /**
     * 根据id来批量查询用户详情
     *  info: 用作额外的筛选条件，如果为空则没有额外筛选条件
     */
    Map<Long, UserInfo> findByIds(List<Long> userIds, UserInfo info);

    // 分页查询
    IPage findAll(Integer page, Integer pagesize);


}
