package com.tanhua.dubbo.api;

import com.tanhua.model.mongo.Video;
import com.tanhua.model.vo.PageResult;

import java.util.List;

public interface VideoApi {

    // 保存视频数据到数据库
    String save(Video video);

    // 根据vid列表查询视频列表
    List<Video> findVideosByVids(List<Long> vids);

    // 分页查询视频列表
    List<Video> queryVideoList(int page, Integer pagesize);

    // 小视频里关注用户
    String focusUser(Long userId, Long followUserId);

    //小视频里取消关注用户
    Long userUnFocus(Long userId, Long unFollowId);

    // 根据某个用户id分页查询
    PageResult findAllVideos(Integer page, Integer pagesize, Long uid);
}