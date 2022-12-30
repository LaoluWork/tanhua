package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.autoconfig.template.OssTemplate;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.MovementApi;
import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.dubbo.api.VisitorsApi;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.mongo.Movement;
import com.tanhua.model.mongo.Visitors;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.model.vo.MovementsVo;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.VisitorsVo;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.*;
import java.io.IOException;
import java.net.CookieHandler;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-01 21:39
 */
@Service
public class MovementService {

    @DubboReference
    private MovementApi movementApi;

    @Autowired
    private OssTemplate ossTemplate;

    @DubboReference
    private UserInfoApi userInfoApi;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DubboReference
    private VisitorsApi visitorsApi;

    @Autowired
    private MqMessageService mqMessageService;

    public void publishMovement(Movement movement, MultipartFile[] imageContent) throws IOException {
        //1.判断发布内容是否存在
        if (StringUtils.isEmpty(movement.getTextContent())) {
            throw new BusinessException(ErrorResult.contentError());
        }
        //2.获取当前用户id
        Long userId = UserHolder.getUserId();
        //3.将文件内容上传阿里云OSS，并且构建出媒体列表
        ArrayList<String> medias = new ArrayList<>();
        for (MultipartFile multipartFile : imageContent) {
            String upload = ossTemplate.upload(multipartFile.getOriginalFilename(), multipartFile.getInputStream());
            medias.add(upload);
        }
        //4.将数据封装到Movement对象
        movement.setUserId(userId);
        movement.setMedias(medias);
        //5.调用API完成发布动态，里面会异步执行创建对应的timeLine表
        String movementId = movementApi.publish(movement);
        //将动态id发送到MQ中，等待自动审核
        mqMessageService.sendAudiMessage(movementId);
    }

    //查询个人动态
    public PageResult findByUserId(Long userId, Integer page, Integer pagesize) {
        //1.根据userid去mongo查询出个人所有动态信息（PageResult--Movement）
        PageResult pr = movementApi.findByUserId(userId, page, pagesize);
        //2.获取pageResult中的item列表
        List<Movement> items = (List<Movement>) pr.getItems();
        //3.非空判断
        if (items == null) {
            return pr;
        }
        //4.循环items列表数据
        UserInfo info = userInfoApi.findById(userId);
        ArrayList<MovementsVo> vos = new ArrayList<>();
        for (Movement item : items) {
            //5.一个Movement构建一个vo对象
            MovementsVo vo = MovementsVo.init(info, item);
            vos.add(vo);
        }
        //6.构建返回值
        pr.setItems(vos);
        return pr;

    }

    //查询好友动态列表
    public PageResult findFriendMovements(Integer page, Integer pagesize) {
        //1.获取用户id
        Long userId = UserHolder.getUserId();
        //2.查询出当前用户好友的动态列表
        List<Movement> list = movementApi.findFriendMovements(page, pagesize, userId);

        //根据动态列表构建vo对象（①需要判断每条动态我是否已经点赞或喜欢，按钮需要高亮信息；②需要查询出对应动态发布人的部分详情信息），封装成pageResult
        return getPageResult(page, pagesize, list);
    }

    // 根据原始的动态表数据，来封装好需要返回movementsVo动态表数据
    private PageResult getPageResult(Integer page, Integer pagesize, List<Movement> list) {
        //3.判断列表是否为空
        if (CollUtil.isEmpty(list)) {
            return new PageResult();
        }
        //4.提取动态列表发布人的id
        List<Long> ids = CollUtil.getFieldValues(list, "userId", Long.class);
        //5.根据id列表查询出好友用户详情
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, null);
        //6.一个movement对象构建一个vo对象
        List<MovementsVo> vos = new ArrayList<>();
        for (Movement movement : list) {
            UserInfo userInfo = map.get(movement.getUserId());
            if (userInfo != null) {
                MovementsVo vo = MovementsVo.init(userInfo, movement);
                //判断自己对于该动态的点赞和喜欢的状态，先判断hashkey是否存在
                String key = Constants.MOVEMENTS_INTERACT_KEY + movement.getId().toHexString();
                String hashKey = Constants.MOVEMENT_LIKE_HASHKEY + UserHolder.getUserId();
                if (redisTemplate.opsForHash().hasKey(key, hashKey)) {
                    vo.setHasLiked(1);
                }
                String loveHashKey = Constants.MOVEMENT_LOVE_HASHKEY + UserHolder.getUserId();
                if (redisTemplate.opsForHash().hasKey(key, loveHashKey)) {
                    vo.setHasLoved(1);
                }
                vos.add(vo);
            }
        }
        //7.构建PageResult并返回
        return new PageResult(page, pagesize, 0L, vos);
    }

    //查询推荐的动态列表
    public PageResult findrRcommendMovements(Integer page, Integer pagesize) {
        //1.去redis中获取推荐的数据id列表
        String redisKey = Constants.MOVEMENTS_RECOMMEND + UserHolder.getUserId();
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        //2.判断数据是否存在
        List<Movement> list;
        if (StringUtils.isEmpty(redisValue)) {
            //3.不存在就随机返回动态数据
            list = movementApi.randomMovements(pagesize);
        } else {
            //4.存在就处理pid数据 "1,23,45,7"
            String[] values = redisValue.split(",");
            List<Long> pids = Arrays.stream(values).skip((page - 1) * pagesize).limit(pagesize)
                    .map(e -> Long.valueOf(e))
                    .collect(Collectors.toList());
            //5.调用api根据pid集合查询动态数据
            list = movementApi.findMovementsByPids(pids);

        }
        //6.调用公共方法构造返回
        return getPageResult(page, pagesize, list);
    }

    //查询单条动态
    public MovementsVo findById(String movementId) {
        // 向RabbitMQ发送消息，记录行为
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0202", "movement", movementId);
        //1.调用api查询出动态详情
        Movement movement = movementApi.findById(movementId);
        //2.封装vo对象
        if (movement != null) {
            UserInfo info = userInfoApi.findById(movement.getUserId());
            MovementsVo vo = MovementsVo.init(info, movement);
            //判断自己对于该动态的点赞和喜欢的状态，先判断hashkey是否存在
            String key = Constants.MOVEMENTS_INTERACT_KEY + movement.getId().toHexString();
            String hashKey = Constants.MOVEMENT_LIKE_HASHKEY + UserHolder.getUserId();
            if (redisTemplate.opsForHash().hasKey(key, hashKey)) {
                vo.setHasLiked(1);
            }
            String loveHashKey = Constants.MOVEMENT_LOVE_HASHKEY + UserHolder.getUserId();
            if (redisTemplate.opsForHash().hasKey(key, loveHashKey)) {
                vo.setHasLoved(1);
            }
            return vo;
        }
        return null;
    }

    /**
     * 查询首页访客列表
     */
    public List<VisitorsVo> queryVisitorsList() {
        //1.先去redis中查询当前用户上次点击访客列表的时间
        String key = Constants.VISITORS_USER;
        String hashKey = String.valueOf(UserHolder.getUserId());
        String value = (String) redisTemplate.opsForHash().get(key, hashKey);
        Long date = StringUtils.isEmpty(value) ? null : Long.valueOf(value);
        //2.调用API查询出访客数据
        List<Visitors> list = visitorsApi.queryVisitorsList(date, UserHolder.getUserId(), 1, 5);
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>();
        }
        //3.提取访客用户的id
        List<Long> ids = CollUtil.getFieldValues(list, "visitorUserId", Long.class);
        //4.查询访客的用户详情
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, null);
        //5.构造vo对象返回
        List<VisitorsVo> vos = new ArrayList<>();
        for (Visitors visitors : list) {
            UserInfo userInfo = map.get(visitors.getVisitorUserId());
            if (userInfo != null) {
                VisitorsVo vo = VisitorsVo.init(userInfo, visitors);
                vos.add(vo);
            }
        }
        return vos;
    }
}
