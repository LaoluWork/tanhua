package com.tanhua.server.service;

import cn.hutool.json.JSONUtil;
import com.tanhua.commons.utils.Constants;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.server.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-15 23:29
 */
@Service
public class UserFreezeService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 判断用户是否被冻结，若被冻结且符合当前请求行为，则抛出异常
     *  参数： 当前操作的行为， 用户id
     */
    public void checkUserStatus(String state, Long userId) {
        //1.拼接redis的key，查询出redis中的数据
        String key = Constants.USER_FREEZE + userId;
        String value = redisTemplate.opsForValue().get(key);
        //2.如果数据存在，判断冻结范围和当前的操作行为是否一致，相同就抛出异常
        if(!StringUtils.isEmpty(value)) {
            Map map = JSONUtil.toBean(value, Map.class);
            String freezingRange = map.get("freezingRange").toString();
            if(state.equals(freezingRange)) {
                throw new BusinessException(ErrorResult.builder().errMessage("用户被冻结").build());
            }
        }
    }
}
