package com.tanhua.dubbo.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tanhua.model.domain.UserInfo;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-28 11:01
 */
public interface BlackListApi {
    //分页查询黑名单列表
    IPage<UserInfo> findByUserId(Long userId, int page, int size);

    //取消黑名单
    void delete(Long userId, Long blackUserId);
}
