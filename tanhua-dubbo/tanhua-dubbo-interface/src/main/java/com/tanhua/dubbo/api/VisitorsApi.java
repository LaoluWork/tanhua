package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.Visitors;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-07 20:14
 */
public interface VisitorsApi {

    //保存访客数据
    void save(Visitors visitors);

    // 查询访客列表
    List<Visitors> queryVisitorsList(Long date, Long userId, Integer page, Integer pagesize);
}
