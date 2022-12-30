package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.Movement;
import com.tanhua.model.vo.PageResult;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-01 21:22
 */
public interface MovementApi {

    //发布动态
    String publish(Movement movement);

    //根据id查询此用户发布的动态
    PageResult findByUserId(Long userId, Integer page, Integer pagesize);

    //根据用户id查询用户的好友动态
    List<Movement> findFriendMovements(Integer page, Integer pagesize, Long userId);

    //随机获取多条动态数据
    List<Movement> randomMovements(Integer counts);

    //根据pid数组查询动态
    List<Movement> findMovementsByPids(List<Long> pids);

    Movement findById(String movementId);

    // 用于后台管理系统查询所有人或指定某个人的动态
    PageResult findByUserId(Long uid, Integer state, Integer page, Integer pagesize);

    //修改动态的state
    void update(String movementId, int state);
}
