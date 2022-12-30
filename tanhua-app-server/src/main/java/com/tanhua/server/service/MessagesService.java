package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.autoconfig.template.HuanXinTemplate;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.CommentApi;
import com.tanhua.dubbo.api.FriendApi;
import com.tanhua.dubbo.api.UserApi;
import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.model.domain.User;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.enums.CommentType;
import com.tanhua.model.mongo.Comment;
import com.tanhua.model.mongo.Friend;
import com.tanhua.model.vo.*;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-04 15:21
 */
@Service
public class MessagesService {

    @DubboReference
    private UserApi userApi;

    @DubboReference
    private UserInfoApi userInfoApi;

    @Autowired
    private HuanXinTemplate huanXinTemplate;

    @DubboReference
    private FriendApi friendApi;

    @DubboReference
    private CommentApi commentApi;

    public UserInfoVo findUserInfoByHuanxin(String huanxinId) {
        //1.根据环信id查询出用户
        User user = userApi.findByHuanxin(huanxinId);
        //2.根据用户id查询出用户详情表
        UserInfo userInfo = userInfoApi.findById(user.getId());
        //3.构建vo返回值
        UserInfoVo vo = new UserInfoVo();
        BeanUtils.copyProperties(userInfo, vo); // 只会copy同名同类型的属性

        if (userInfo.getAge() != null) {
            vo.setAge(userInfo.getAge().toString());
        }
        return vo;
    }

    // 添加好友
    public void contacts(Long friendId) {
        //1.将好友关系添加到环信种
        Boolean aBoolean = huanXinTemplate.addContact(Constants.HX_USER_PREFIX + UserHolder.getUserId(),
                Constants.HX_USER_PREFIX + friendId);
        if (!aBoolean) {
            throw new BusinessException(ErrorResult.error());
        }
        //2.如果环信注册成功，那就将好友数据添加到mongo中
        friendApi.save(UserHolder.getUserId(), friendId);

    }

    // 查询联系人列表，可按照关键字
    public PageResult findFriends(Integer page, Integer pagesize, String keyword) {
        //1.先根据id查询出好友数据
        List<Friend> list = friendApi.findByUserId(UserHolder.getUserId(), page, pagesize);
        if (CollUtil.isEmpty(list)) {
            return new PageResult();
        }
        //2.提取出好友数据中的好友id列表
        List<Long> ids = CollUtil.getFieldValues(list, "friendId", Long.class);
        //3.根据好友id列表查询出用户详情
        UserInfo info = new UserInfo();
        info.setNickname(keyword);
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, info);
        //4.构建vo对象
        ArrayList vos = new ArrayList();
        for (Friend friend : list) {
            UserInfo userInfo = map.get(friend.getFriendId());
            if (userInfo != null) {
                ContactVo vo = ContactVo.init(userInfo);
                vos.add(vo);
            }
        }

        return new PageResult(page, pagesize, 0l, vos);
    }

    // 查询自己动态的互动情况列表
    public PageResult findInteraction(Integer page, Integer pagesize, CommentType type) {
        //1.根据用户id作为publishUserId去数据库查询谁对我的动态点过赞
        PageResult pr = commentApi.findByPublishUserId(page, pagesize, UserHolder.getUserId(), type);
        //2.判断是否为空
        List<Comment> list = (List<Comment>) pr.getItems();
        if(CollUtil.isEmpty(list)){
            return new PageResult();
        }
        //3.非空就提取出列表中的userId,这是点赞人的id
        List<Long> ids = CollUtil.getFieldValues(list, "userId", Long.class);
        //4.调用api查询用户详情
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, null);
        //5.构建vo对象，封装并返回
        List<CommentVo> vos = new ArrayList<>();
        for (Comment comment : list) {
            UserInfo info = map.get(comment.getUserId());
            CommentVo vo = CommentVo.init(info, comment);
            vos.add(vo);
        }
        pr.setItems(vos);
        return pr;
    }
}
