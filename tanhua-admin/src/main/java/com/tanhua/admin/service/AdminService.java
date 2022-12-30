package com.tanhua.admin.service;

import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tanhua.admin.exception.BusinessException;
import com.tanhua.admin.interceptor.AdminHolder;
import com.tanhua.admin.mapper.AdminMapper;
import com.tanhua.commons.utils.Constants;
import com.tanhua.commons.utils.JwtUtils;
import com.tanhua.model.domain.Admin;
import com.tanhua.model.vo.AdminVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    //用户登录校验
    public Map login(Map map) {
        //1.获取map中参数
        String username = (String)map.get("username");
        String password = (String)map.get("password");
        String verificationCode = (String)map.get("verificationCode");
        String uuid = (String)map.get("uuid");
        //2.获取redis中验证码，检验是否正确
        String key = Constants.CAP_CODE + uuid;
        String redisCode = redisTemplate.opsForValue().get(key);
        if(StringUtils.isEmpty(redisCode) || !redisCode.equals(verificationCode)){
            throw new BusinessException("验证码错误！");
        }
        //验证码正确之后就删除
        redisTemplate.delete(key);
        //3.根据用户名查询数据
        QueryWrapper<Admin> qw = new QueryWrapper<Admin>().eq("username", username);
        Admin admin = adminMapper.selectOne(qw);
        //4.判断用户是否存在和用户密码是否正确
        password = SecureUtil.md5(password);
        if(admin == null || !admin.getPassword().equals(password)){
            throw new BusinessException("用户不存在或密码错误");
        }
        //5.生成token数据
        Map tokenMap = new HashMap();
        tokenMap.put("username", username);
        tokenMap.put("id", admin.getId());
        String token = JwtUtils.getToken(tokenMap);
        //6.构造返回值
        Map retMap = new HashMap();
        retMap.put("token", token);
        return retMap;
    }

    //登录后获取用户的信息
    public AdminVo profile() {
        Long userId = AdminHolder.getUserId();
        Admin admin = adminMapper.selectById(userId);
        return AdminVo.init(admin);
    }
}
