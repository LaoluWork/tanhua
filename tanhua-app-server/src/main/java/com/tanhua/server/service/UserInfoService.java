package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.autoconfig.template.AipFaceTemplate;
import com.tanhua.autoconfig.template.OssTemplate;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.dubbo.api.UserLikeApi;
import com.tanhua.dubbo.api.VisitorsApi;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.mongo.UserLike;
import com.tanhua.model.mongo.Visitors;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.UserInfoVo;
import com.tanhua.model.vo.VisitorsInfoVo;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-08-15 9:56
 */
@Service
public class UserInfoService {

    @DubboReference
    private UserInfoApi userInfoApi;

    @Autowired
    private OssTemplate ossTemplate;

    @Autowired
    private AipFaceTemplate aipFaceTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DubboReference
    private VisitorsApi visitorsApi;

    @DubboReference
    private UserLikeApi userLikeApi;

    public void save(UserInfo userInfo) {
        userInfoApi.save(userInfo);
    }

    /**
     * 上传头像
     *
     * @param headPhoto
     * @param id
     * @throws IOException
     */
    public void updateHead(MultipartFile headPhoto, Long id) throws IOException {
        //1.将图片上传到阿里云oss
        String imageUrl = ossTemplate.upload(headPhoto.getOriginalFilename(), headPhoto.getInputStream());
        //2.调用百度云判断是否包含人脸
        boolean detect = aipFaceTemplate.detect(imageUrl);
        //2.1 如果不包含人脸，抛出异常
        if (!detect) {
            throw new BusinessException(ErrorResult.faceError());
        }
        //2.2 包含人脸，调用API更新数据库
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setAvatar(imageUrl);
        userInfoApi.update(userInfo);
    }

    /**
     * 根据userid去数据库查询userinfo信息，返回的时候要将其变成userInfoVo对象才能被手机接收
     */
    public UserInfoVo findById(Long userID) {
        UserInfo userinfo = userInfoApi.findById(userID);
        //封装成userInfoVo对象
        UserInfoVo vo = new UserInfoVo();
        BeanUtils.copyProperties(userinfo, vo); // 只会copy同名同类型的属性

        if (userinfo.getAge() != null) {
            vo.setAge(userinfo.getAge().toString());
        }
        return vo;
    }

    public void update(UserInfo userinfo) {
        userInfoApi.update(userinfo);
    }


    //查询访客的详情列表
    public PageResult queryVisitorsInfo(Integer page, Integer pagesize) {
        //1.根据当前用户id查询出需要展示的访客列表List<Visitors>
        //先去redis中查询当前用户上次点击访客列表的时间
        String key = Constants.VISITORS_USER;
        String hashKey = String.valueOf(UserHolder.getUserId());
        String value = (String) redisTemplate.opsForHash().get(key, hashKey);
        Long date = StringUtils.isEmpty(value) ? null : Long.valueOf(value);
        //调用API查询出访客数据
        List<Visitors> list = visitorsApi.queryVisitorsList(date, UserHolder.getUserId(), page, pagesize);
        //因为当前点击查询了一次，所以需要更新redis中的查看访客时间
        redisTemplate.opsForHash().put(key, hashKey, String.valueOf(System.currentTimeMillis()));
        //2.判断列表是否为空
        if (CollUtil.isEmpty(list)) {
            return new PageResult();
        }
        //3.提取列表中的userId列表，并根据id列表查询出用户详情
        List<Long> ids = CollUtil.getFieldValues(list, "visitorUserId", Long.class);
        Map<Long, UserInfo> infoMap = userInfoApi.findByIds(ids, null);
        //4.查询当前用户是否喜欢上面列表中的用户（区分已喜欢的用户和未喜欢用户的记录）
        Map<Long, UserLike> userLikeMap = userLikeApi.queryByLikeUserIds(UserHolder.getUserId(), ids);
        //5.构建vo对象返回
        List<VisitorsInfoVo> vos = new ArrayList<>();
        if (CollUtil.isEmpty(userLikeMap)) {
            for (Visitors visitors : list) {
                Long visitorUserId = visitors.getVisitorUserId();
                UserInfo userInfo = infoMap.get(visitorUserId);
                VisitorsInfoVo vo = VisitorsInfoVo.init(userInfo, visitors, null);
                vos.add(vo);
            }
        } else {
            for (Visitors visitors : list) {
                Long visitorUserId = visitors.getVisitorUserId();
                UserInfo userInfo = infoMap.get(visitorUserId);
                UserLike userLike = userLikeMap.get(visitorUserId);
                VisitorsInfoVo vo = VisitorsInfoVo.init(userInfo, visitors, userLike);
                vos.add(vo);
            }
        }

        return new PageResult(page, pagesize, 0L, vos);
    }
}
