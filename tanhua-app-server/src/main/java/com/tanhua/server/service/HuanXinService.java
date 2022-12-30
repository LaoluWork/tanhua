package com.tanhua.server.service;

import com.tanhua.dubbo.api.UserApi;
import com.tanhua.model.domain.User;
import com.tanhua.model.vo.HuanXinUserVo;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-04 14:58
 */
@Service
public class HuanXinService {

    @DubboReference
    private UserApi userApi;

    //查询环信的用户数据
    public HuanXinUserVo findHuanXinUser() {
        Long userId = UserHolder.getUserId();
        User user = userApi.findById(userId);
        if (user == null) {
            return null;
        }
        return new HuanXinUserVo(user.getHxUser(), user.getHxPassword());

    }
}
