package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.tanhua.autoconfig.template.HuanXinTemplate;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.*;
import com.tanhua.model.domain.Question;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.dto.RecommendUserDto;
import com.tanhua.model.mongo.RecommendUser;
import com.tanhua.model.mongo.Visitors;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.model.vo.NearUserVo;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.TodayBest;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-30 20:42
 */
@Slf4j
@Service
public class TanhuaService {

    @DubboReference
    private RecommendUserApi recommendUserApi;

    @DubboReference
    private UserInfoApi userInfoApi;

    @DubboReference
    private QuestionApi questionApi;

    @Autowired
    private HuanXinTemplate huanXinTemplate;

    @Value("${tanhua.default.recommend.users}")
    private String recommendUser;

    @DubboReference
    private UserLikeApi userLikeApi;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private MessagesService messagesService;

    @DubboReference
    private UserLocationApi userLocationApi;

    @DubboReference
    private VisitorsApi visitorsApi;

    // 查询今日佳人数据
    public TodayBest todayBest() {
        //1.获取用户id
        Long userId = UserHolder.getUserId();
        //2.调用api查询出recommendUser
        RecommendUser recommendUser = recommendUserApi.queryWithMaxScore(userId);
        if (recommendUser == null) {
             recommendUser = new RecommendUser();
             recommendUser.setUserId(1L);
             recommendUser.setScore(99d);
        }
        //3.将RecommendUser转化为TodayBest对象
        UserInfo userinfo = userInfoApi.findById(recommendUser.getUserId());

        TodayBest vo = TodayBest.init(userinfo, recommendUser);
        //4.返回
        return vo;

    }

//    // 根据前端传来的dto对象来查询分页推荐好友列表
//    public PageResult recommendation(RecommendUserDto dto) {
//        //1.获取用户id
//        Long userId = UserHolder.getUserId();
//        //2.查询出当前用户在MongoDB中所有的推荐好友列表，并且是以PageResult返回
//        PageResult pr = recommendUserApi.queryRecommendUserList(dto.getPage(), dto.getPagesize(), userId);
//        //3.获取pr中的用户列表，并判断是否为空
//        List<RecommendUser> items = (List<RecommendUser>) pr.getItems();
//        if (items == null) {
//            return pr;
//        }
//        //4.不为空就遍历列表，筛选出符合dto条件的用户，且保存到list中
//        ArrayList<TodayBest> list = new ArrayList<>();
//        for (RecommendUser item : items) {
//            UserInfo userInfo = userInfoApi.findById(item.getUserId());
//            //根据dto条件来筛选
//            if (!StringUtils.isEmpty(dto.getGender()) && !dto.getGender().equals(userInfo.getGender())) {
//                continue;
//            }
//            if (dto.getAge() != null && dto.getAge() < userInfo.getAge()) {
//                continue;
//            }
//            TodayBest vo = TodayBest.init(userInfo, item);
//            list.add(vo);
//        }
//        //5.将符合条件的用户封装回PageResult
//        pr.setItems(list);
//        //6.返回结果
//        return pr;
//    }


    // 根据前端传来的dto对象来查询分页推荐好友列表
    public PageResult recommendation(RecommendUserDto dto) {
        //1.获取用户id
        Long userId = UserHolder.getUserId();
        //2.查询出当前用户在MongoDB中所有的推荐好友列表，并且是以PageResult返回
        PageResult pr = recommendUserApi.queryRecommendUserList(dto.getPage(), dto.getPagesize(), userId);
        //3.获取pr中的用户列表，并判断是否为空
        List<RecommendUser> items = (List<RecommendUser>) pr.getItems();
        if (items == null) {
            return pr;
        }
        //4.构建出要批量查询的id列表
        List<Long> ids = CollUtil.getFieldValues(items, "userId", Long.class);
        //5.构建查询条件，并且批量查询
        UserInfo userInfo = new UserInfo();
        userInfo.setAge(dto.getAge());
        userInfo.setGender(dto.getGender());
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, userInfo);
        //6.根据map来组装成list，并且设置到pr中
        ArrayList<TodayBest> list = new ArrayList<>();
        for (RecommendUser item : items) {
            UserInfo info = map.get(item.getUserId());
            if (info != null) {
                TodayBest vo = TodayBest.init(info, item);
                list.add(vo);
            }
        }
        pr.setItems(list);

        //7.返回结果
        return pr;
    }

    // 查看佳人信息
    public TodayBest personalInfo(Long userId) {
        //1.根据传来的用户id查询用户详情
        UserInfo userInfo = userInfoApi.findById(userId);
        //2.根据用户id和当前操作人的id，查询两者的推荐数据
        RecommendUser user = recommendUserApi.queryByUserId(userId, UserHolder.getUserId());

        //构造访客数据，调用api保存
        Visitors visitors = new Visitors();
        visitors.setUserId(userId);
        visitors.setVisitorUserId(UserHolder.getUserId());
        visitors.setDate(System.currentTimeMillis());
        visitors.setFrom("首页");
        visitors.setVisitDate(new SimpleDateFormat("yyyyMMdd").format(new Date()));
        visitors.setScore(user.getScore());
        visitorsApi.save(visitors);

        //3.构造返回值
        return TodayBest.init(userInfo, user);
    }


    // 查询陌生人问题
    public String strangerQuestions(Long userId) {
        Question question = questionApi.findByUserId(userId);
        return question == null ? "你对老卢感觉怎么样？" : question.getTxt();
    }

    // 回复陌生人问题
    public void replyQuestions(Long targetUserId, String reply) {
        //1.构造Json数据
        Long currentUserId = UserHolder.getUserId();
        UserInfo userInfo = userInfoApi.findById(currentUserId);
        HashMap map = new HashMap();
        map.put("userId", currentUserId);
        map.put("huanXinId", Constants.HX_USER_PREFIX + currentUserId);
        map.put("nickname", userInfo.getNickname());
        map.put("strangerQuestion", strangerQuestions(targetUserId));
        map.put("reply", reply);
        String message = JSON.toJSONString(map);
        //2.调用template通过环信发送消息
        Boolean aBoolean = huanXinTemplate.sendMsg(Constants.HX_USER_PREFIX + targetUserId, message);
        if (!aBoolean) {
            throw  new BusinessException(ErrorResult.error());
        }
    }

    //探花-推荐用户列表(卡片）
    public List<TodayBest> queryCardsList() {
        //1.调用api查询出当前用户的推荐数据，排除掉已经喜欢或不喜欢的用户数据
        List<RecommendUser> users = recommendUserApi.QueryCardsList(UserHolder.getUserId(), 10);
        //2.如果数据列表为空，那就要构造数据
        if (CollUtil.isEmpty(users)){
            users = new ArrayList<>();
            String[] userIds = recommendUser.split(",");
            for (String userId : userIds) {
                RecommendUser recommendUser = new RecommendUser();
                recommendUser.setUserId(Convert.toLong(userId));
                recommendUser.setToUserId(UserHolder.getUserId());
                recommendUser.setScore(RandomUtil.randomDouble(60, 90));
                users.add(recommendUser);
            }
        }
        //3.提取推荐用户id列表，查询用户详情，构造vo对象，返回
        List<Long> ids = CollUtil.getFieldValues(users, "userId", Long.class);
        Map<Long, UserInfo> infoMap = userInfoApi.findByIds(ids, null);

        List<TodayBest> vos = new ArrayList<>();
        for (RecommendUser user : users) {
            UserInfo info = infoMap.get(user.getUserId());
            TodayBest vo = TodayBest.init(info, user);
            vos.add(vo);
        }
        return vos;
    }

    //探花功能的划片喜欢
    public void likeUser(Long likeUserId) {
        //1.先更新或新增在mongodb中的喜欢数据
        Boolean save = userLikeApi.saveOrUpdate(UserHolder.getUserId(), likeUserId, true);
        if (!save) {
            throw new BusinessException(ErrorResult.error());
        }
        //2.更新redis中的数据，先删除不喜欢的数据（有可能之前在ta主页点击了不喜欢），再添加喜欢的数据
        redisTemplate.opsForSet().remove(Constants.USER_NOT_LIKE_KEY + UserHolder.getUserId(), likeUserId.toString());
        redisTemplate.opsForSet().add(Constants.USER_LIKE_KEY + UserHolder.getUserId(), likeUserId.toString());
        //3.判断对方是否也喜欢自己
        if(isLike(likeUserId, UserHolder.getUserId())) {
            //4.如果双向喜欢就添加为好友
            messagesService.contacts(likeUserId);
        }

    }
    public Boolean isLike(Long userId, Long likeUserId) {
        // 查询redis中的喜欢set中是否含有对方id
        return redisTemplate.opsForSet().isMember(Constants.USER_LIKE_KEY + userId, likeUserId.toString());
    }

    // 不喜欢用户
    public void notLikeUser(Long likeUserId) {
        //1.先更新或新增在mongodb中的喜欢数据
        Boolean save = userLikeApi.saveOrUpdate(UserHolder.getUserId(), likeUserId, false);
        if (!save) {
            throw new BusinessException(ErrorResult.error());
        }
        //2.更新redis中的数据，先删除喜欢的数据，再添加不喜欢的数据
        redisTemplate.opsForSet().add(Constants.USER_NOT_LIKE_KEY + UserHolder.getUserId(), likeUserId.toString());
        redisTemplate.opsForSet().remove(Constants.USER_LIKE_KEY + UserHolder.getUserId(), likeUserId.toString());
    }

    //查询附近的人
    public List<NearUserVo> queryNearUser(String gender, String distance) {
        //1.先去数据库查询出附近的用户id（会包含自己的id）
        List<Long> userIds = userLocationApi.queryNearUser(UserHolder.getUserId(), Double.valueOf(distance));

        //2.判断列表是否为空
        if (CollUtil.isEmpty(userIds)) {
            return new ArrayList<>();
        }
        //3.不为空就调用userInfoApi查询用户详情
        UserInfo userInfo = new UserInfo();
        userInfo.setGender(gender);
        Map<Long, UserInfo> map = userInfoApi.findByIds(userIds, userInfo);
        //4.构造vo对象
        List<NearUserVo> vos = new ArrayList<>();
        for (Long userId : userIds) {
            //排除当前用户
            if(userId.equals(UserHolder.getUserId()) ){
                continue;
            }
            UserInfo info = map.get(userId);
            if(info != null) {
                NearUserVo vo = NearUserVo.init(info);
                vos.add(vo);
            }
        }
        return vos;
    }
}
