package com.tanhua.dubbo.api;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-06 20:10
 */
public interface UserLocationApi {


    //更新地理位置
    Boolean updateLocation(Long userId, Double longitude, Double latitude, String address);

    //查询附近的人id
    List<Long> queryNearUser(Long userId, Double valueOf);
}
