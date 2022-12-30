package com.tanhua.server.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tanhua.autoconfig.template.SmsTemplate;
import com.tanhua.dubbo.api.BlackListApi;
import com.tanhua.dubbo.api.QuestionApi;
import com.tanhua.dubbo.api.SettingsApi;
import com.tanhua.dubbo.api.UserApi;
import com.tanhua.model.domain.Question;
import com.tanhua.model.domain.Settings;
import com.tanhua.model.domain.User;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.SettingsVo;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-28 11:10
 */
@Service
public class SettingsService {

    @DubboReference
    private QuestionApi questionApi;

    @DubboReference
    private SettingsApi settingsApi;

    @DubboReference
    private BlackListApi blackListApi;

    @Autowired
    private SmsTemplate smsTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    @DubboReference
    private UserApi userApi;

    /**
     * 查询通用设置，返回settingsVo对象给前端
     * @return
     */
    public SettingsVo settings() {
        SettingsVo vo = new SettingsVo();
        //1.获取用户id
        Long userId = UserHolder.getUserId();
        vo.setId(userId);
        //2.获取用户手机号码
        vo.setPhone(UserHolder.getMobile());
        //3.获取用户陌生人问题
        Question question = questionApi.findByUserId(userId);
        String txt = question == null ? "你喜欢Java吗？" : question.getTxt();
        vo.setStrangerQuestion(txt);
        //4.获取用户的APP通知开关数据
        Settings settings = settingsApi.findByUserId(userId);
        if (settings != null) {
            vo.setGonggaoNotification(settings.getGonggaoNotification());
            vo.setLikeNotification(settings.getLikeNotification());
            vo.setPinglunNotification(settings.getPinglunNotification());

        }
        return vo;

    }

    //设置陌生人问题
    public void saveQuestion(String content) {
        //1.获取用户id
        Long userId = UserHolder.getUserId();
        //2.调用api查询当前用户的陌生人问题
        Question question = questionApi.findByUserId(userId);
        //3.判断问题是否存在于数据库
        if (question == null) {
            //3.1 如果不存在，保存
            question = new Question();
            question.setUserId(userId);
            question.setTxt(content);
            questionApi.save(question);
        }else{
            //3.2 如果存在，更新
            question.setTxt(content);
            questionApi.update(question);
        }
    }

    // 通知设置
    public void saveSettings(Map map) {
        Boolean likeNotification = (Boolean) map.get("likeNotification");
        Boolean pinglunNotification = (Boolean) map.get("pinglunNotification");
        Boolean gonggaoNotification = (Boolean) map.get("gonggaoNotification");
        //1.获取当前用户id
        Long userId = UserHolder.getUserId();
        //2.根据用户id，查询用户的通知设置
        Settings settings = settingsApi.findByUserId(userId);
        //3.判断是否为空
        if (settings == null) {
            settings = new Settings();
            settings.setUserId(userId);
            settings.setLikeNotification(likeNotification);
            settings.setPinglunNotification(pinglunNotification);
            settings.setGonggaoNotification(gonggaoNotification);
            settingsApi.save(settings);
        }else {
            settings.setLikeNotification(likeNotification);
            settings.setPinglunNotification(pinglunNotification);
            settings.setGonggaoNotification(gonggaoNotification);
            settingsApi.update(settings);
        }

    }

    //分页查询黑名单列表
    public PageResult blacklist(int page, int size) {
        //1.获取当前用户id
        Long userId = UserHolder.getUserId();
        //2.调用API查询用户的黑名单分页列表 Ipage对象
        IPage<UserInfo> iPage = blackListApi.findByUserId(userId, page, size);
        //3.对象转化，将查询返回的Ipage对象的内容封装到PageResult中
        PageResult pageResult = new PageResult(page,size, iPage.getTotal(), iPage.getRecords());
        //4.返回
        return pageResult;
    }

    //取消黑名单
    public void deleteBlackList(Long blackUserId) {
        //1.获取当前用户id
        Long userId = UserHolder.getUserId();
        //2.调用api删除
        blackListApi.delete(userId, blackUserId);
    }

    //修改绑定手机号时先发送验证码
    public void sendCode() {
        //1.发送验证码到手机
//        smsTemplate.sendSms(UserHolder.getMobile(), RandomStringUtils.randomNumeric(6));
        String phone = UserHolder.getMobile();
        String code = "123456";
        //2.将验证码保存到redis中
        redisTemplate.opsForValue().set("CHANGE_CHECK_CODE_"+phone,code, Duration.ofMinutes(5));

    }

    //校验验证码
    public boolean checkCode(String verificationCode) {
        //1.先从redis中获取验证码
        String phone = UserHolder.getMobile();
        String code = (String) redisTemplate.opsForValue().get("CHANGE_CHECK_CODE_" + phone);
        //2.判断是否存在
        //2.1如果不存在，直接返回
        if (code == null) {
            return false;
        }
        //2.2如果存在,判断是否符合
        if (!verificationCode.equals(code)) {
            return false;
        }

        //删除redis中的验证码
        redisTemplate.delete("CHANGE_CHECK_CODE_" + phone);

        return true;
    }

    //保存新手机号
    public void saveNewPhone(String phone) {
        //1.修改ThreadLocal中的moblie
        User user = new User();
        user.setMobile(phone);
        user.setId(UserHolder.getUserId());
        UserHolder.set(user);
        //2.修改user表
        userApi.update(user);
    }
}
