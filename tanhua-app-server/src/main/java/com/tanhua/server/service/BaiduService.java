package com.tanhua.server.service;

import com.tanhua.dubbo.api.UserLocationApi;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-06 20:13
 */
@Service
public class BaiduService {

    @DubboReference
    private UserLocationApi userLocationApi;

    // 更新地理位置信息
    public void updateLocation(Double longitude, Double latitude, String address) {
        Boolean flag = userLocationApi.updateLocation(UserHolder.getUserId(), longitude, latitude, address);

        if(!flag) {
            throw new BusinessException(ErrorResult.error());
        }
    }
}
