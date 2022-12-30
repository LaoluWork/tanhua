package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.Friend;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-04 22:16
 */
public interface FriendApi {
    //添加好友
    void save(Long userId, Long friendId);

    //查询好友列表
    List<Friend> findByUserId(Long userId, Integer page, Integer pagesize);
}
