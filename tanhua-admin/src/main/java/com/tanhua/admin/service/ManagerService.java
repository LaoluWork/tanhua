package com.tanhua.admin.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.MovementApi;
import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.dubbo.api.VideoApi;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.mongo.Movement;
import com.tanhua.model.vo.MovementsVo;
import com.tanhua.model.vo.PageResult;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-14 22:04
 */
@Service
public class ManagerService {

    @DubboReference
    private UserInfoApi userInfoApi;

    @DubboReference
    private VideoApi videoApi;

    @DubboReference
    private MovementApi movementApi;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    //查询首页用户列表
    public PageResult findAllUsers(Integer page, Integer pagesize) {
        IPage<UserInfo> iPage = userInfoApi.findAll(page, pagesize);
        List<UserInfo> list = iPage.getRecords();
        for (UserInfo userInfo : list) {
            //要判断用户的冻结状态
            String key = Constants.USER_FREEZE + userInfo.getId();
            if(redisTemplate.hasKey(key)) {
                userInfo.setUserStatus("2");
            }
        }
        return new PageResult(page, pagesize, iPage.getTotal(), list);
    }

    public UserInfo findUserById(Long userId) {
        UserInfo userInfo = userInfoApi.findById(userId);
        //要判断用户的冻结状态
        String key = Constants.USER_FREEZE + userId;
        if(redisTemplate.hasKey(key)) {
            userInfo.setUserStatus("2");
        }
        return userInfo;
    }

    public PageResult findAllVideos(Integer page, Integer pagesize, Long uid) {
        return videoApi.findAllVideos(page, pagesize, uid);
    }

    public PageResult findAllMovements(Integer page, Integer pagesize, Long uid, Integer state) {
        //1.调用API查询出动态列表的PageResult
        PageResult result = movementApi.findByUserId(uid, state, page, pagesize);
        List<Movement> items = (List<Movement>) result.getItems();
        if(CollUtil.isEmpty(items)){
            return new PageResult();
        }
        //2.提取userId列表，查询用户详情列表
        List<Long> ids = CollUtil.getFieldValues(items, "userId", Long.class);
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, null);
        //3.构造vo对象
        List<MovementsVo> vos = new ArrayList<>();
        for (Movement movement : items) {
            UserInfo info = map.get(movement.getUserId());
            if (info != null) {
                MovementsVo vo = MovementsVo.init(info, movement);
                vos.add(vo);
            }
        }
        //4.返回
        result.setItems(vos);
        return result;
    }

    //查询单条动态
    public MovementsVo findMovementById(String movementId) {
        //1.调用api查询出动态详情
        Movement movement = movementApi.findById(movementId);
        //2.封装vo对象
        if (movement != null) {
            UserInfo info = userInfoApi.findById(movement.getUserId());
            MovementsVo vo = MovementsVo.init(info, movement);
            return vo;
        }
        return null;
    }

    //用户冻结
    public Map userFreeze(Map params) {
        //1.构造redis的冻结key，判断是冻结几天
        String userId = params.get("userId").toString();
        String key = Constants.USER_FREEZE + userId;
        //冻结时间，1为冻结3天，2为冻结7天，3为永久冻结
        Integer freezingTime = Integer.valueOf(params.get("freezingTime").toString());
        int days = 0;
        if(freezingTime == 1) {
            days = 3;
        }else if (freezingTime == 2){
            days = 7;
        }
        //2.向redis中设置对应值
        String value = JSONUtil.toJsonStr(params);
        if(days > 0) {
            redisTemplate.opsForValue().set(key, value, days, TimeUnit.DAYS);
        }else {
            redisTemplate.opsForValue().set(key, value);
        }
        //3.构造返回值
        HashMap map = new HashMap();
        map.put("message", "冻结成功");
        return map;
    }


    //用户解冻
    public Map userUnfreeze(Map params) {
        //1.构造redis的key
        String userId = params.get("userId").toString();
        String key = Constants.USER_FREEZE + userId;
        //2.删除redis中对应的值
        redisTemplate.delete(key);
        HashMap map = new HashMap();
        map.put("message", "解冻成功");
        return map;
    }
}
