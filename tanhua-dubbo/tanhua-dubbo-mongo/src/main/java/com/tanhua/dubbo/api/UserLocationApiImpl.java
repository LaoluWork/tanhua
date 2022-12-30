package com.tanhua.dubbo.api;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.model.mongo.UserLocation;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-06 20:10
 */
@DubboService
public class UserLocationApiImpl implements  UserLocationApi{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Boolean updateLocation(Long userId, Double longitude, Double latitude, String address) {
        try {
            //1.根据用户id查询数据库中对应的地理位置数据
            Query query = Query.query(Criteria.where("userId").is(userId));
            UserLocation location = mongoTemplate.findOne(query, UserLocation.class);
            if(location == null) {
                //2.如果不存在就保存
                location = new UserLocation();
                location.setLocation(new GeoJsonPoint(longitude, latitude));
                location.setUserId(userId);
                location.setAddress(address);
                location.setUpdated(System.currentTimeMillis());
                location.setLastUpdated(System.currentTimeMillis());
                mongoTemplate.save(location);
            }else {
                //3.存在就更新
                Update update = Update.update("location", new GeoJsonPoint(longitude, latitude))
                        .set("address", address)
                        .set("updated", System.currentTimeMillis())
                        .set("lastUpdated", location.getUpdated());
                mongoTemplate.updateFirst(query, update, UserLocation.class);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //查询附近的人id
    @Override
    public List<Long> queryNearUser(Long userId, Double meter) {
        //1.先查出当前用户的位置数据
        Query query = Query.query(Criteria.where("userId").is(userId));
        UserLocation location = mongoTemplate.findOne(query, UserLocation.class);
        if (location == null) {
            //2.如果当前用户没有位置数据，直接返回
            return null;
        }
        //3.得到原点
        GeoJsonPoint point = location.getLocation();
        //4.绘制半径
        Distance distance = new Distance(meter / 1000, Metrics.KILOMETERS);
        //5.绘制圆
        Circle circle = new Circle(point, distance);
        //6.查询
        Query locationQuery = Query.query(Criteria.where("location").withinSphere(circle));
        List<UserLocation> list = mongoTemplate.find(locationQuery, UserLocation.class);

        return CollUtil.getFieldValues(list, "userId", Long.class);
    }
}
