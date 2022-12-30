package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.PageUtil;
import com.github.tobato.fastdfs.domain.conn.FdfsWebServer;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.tanhua.autoconfig.template.OssTemplate;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.dubbo.api.VideoApi;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.mongo.Video;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.VideoVo;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-11 15:03
 */
@Service
public class SmallVideosService {

    @Autowired
    private FastFileStorageClient client;

    @Autowired
    private FdfsWebServer webServer;

    @DubboReference
    private VideoApi videoApi;

    @Autowired
    private OssTemplate ossTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @DubboReference
    private UserInfoApi userInfoApi;

    @Autowired
    private MqMessageService mqMessageService;


    /**
    * @Description: 上传视频发布
     * @Param: videoThumbnail  封面图片
     * @Param: videoFile  视频文件
     */
    public void saveVideos(MultipartFile videoThumbnail, MultipartFile videoFile) throws IOException {
        // 1.将视频上传到FastDFS，获取访问url
        String filename = videoFile.getOriginalFilename();
        String fileSuffix = filename.substring(filename.lastIndexOf(".") + 1);
        StorePath storePath = client.uploadFile(videoFile.getInputStream(), videoFile.getSize(), fileSuffix, null);
        String videoUrl = storePath.getFullPath() + webServer.getWebServerUrl();
        // 2.将封面图片上传到oss， 获取访问的url
        String imageUrl = ossTemplate.upload(videoThumbnail.getOriginalFilename(), videoThumbnail.getInputStream());
        // 3.构建Videos对象
        Video video = new Video();
        video.setUserId(UserHolder.getUserId());
        video.setPicUrl(imageUrl);
        video.setVideoUrl(videoUrl);
        video.setText("老子就是不一样的火！");
        // 4.调用API保存video数据到mongodb中
        String videoId = videoApi.save(video);
        if (StringUtils.isEmpty(videoId)) {
            throw new BusinessException(ErrorResult.error());
        }
        // 记录用户行为，为推荐系统提供数据支撑，0301为发小视频
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0301", "video", videoId);
    }

    //查询视频列表
    @Cacheable(
            value = "videos",
            key = "T(com.tanhua.server.interceptor.UserHolder).getUserId() + '_' + #page + '_' + #pagesize"
    )
    public PageResult queryVideoList(Integer page, Integer pagesize) {

        //1、查询redis数据
        String redisKey = Constants.VIDEOS_RECOMMEND +UserHolder.getUserId();
        String redisValue = redisTemplate.opsForValue().get(redisKey);
        //2、判断redis数据是否存在，判断redis中数据是否满足本次分页条数
        List<Video> list = new ArrayList<>();
        int redisPages = 0;
        if(!StringUtils.isEmpty(redisValue)) {
            //3、如果redis数据存在，根据VID查询数据
            String[] values = redisValue.split(",");
            //判断当前页的起始条数是否小于数组总数
            if( (page -1) * pagesize < values.length) {
                List<Long> vids = Arrays.stream(values).skip((page - 1) * pagesize).limit(pagesize)
                        .map(e->Long.valueOf(e))
                        .collect(Collectors.toList());
                //5、调用API根据PID数组查询动态数据
                list = videoApi.findVideosByVids(vids);
            }
            redisPages = PageUtil.totalPage(values.length,pagesize);
        }
        //4、如果redis数据不存在，分页查询视频数据
        if(list.isEmpty()) {
            //page的计算规则，  传入的页码  -- redis查询的总页数
            list = videoApi.queryVideoList(page - redisPages, pagesize);  //page=1 ?
        }
        //5、提取视频列表中所有的用户id
        List<Long> userIds = CollUtil.getFieldValues(list, "userId", Long.class);
        //6、查询用户信息
        Map<Long, UserInfo> map = userInfoApi.findByIds(userIds, null);
        //7、构建返回值
        List<VideoVo> vos = new ArrayList<>();
        for (Video video : list) {
            UserInfo info = map.get(video.getUserId());
            if(info != null) {
                VideoVo vo = VideoVo.init(info, video);
                String key = Constants.FOCUS_USER + UserHolder.getUserId();
                if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, video.getUserId().toString()))) {
                    vo.setHasFocus(1);
                }
                vos.add(vo);
            }
        }

        return new PageResult(page,pagesize,0L,vos);
    }

    // 小视频里关注用户
    public void focusUser(Long followUserId) {
        // 1. 将关注数据保存到mongodb中
        String focusId = videoApi.focusUser(UserHolder.getUserId(), followUserId);
        // 判断是否关注成功
        if (StringUtils.isEmpty(focusId)) {
            throw new BusinessException(ErrorResult.error());
        }
        // 2. 将当前用户对关注用户的信息保存到redis中
        String key = Constants.FOCUS_USER + UserHolder.getUserId();
        redisTemplate.opsForSet().add(key, followUserId.toString());
    }

    // 小视频里取消关注用户
    public void userUnFocus(Long unFollowId) {
        // 1.删除数据库中的数据
        Long count = videoApi.userUnFocus(UserHolder.getUserId(), unFollowId);
        if (count == 0) {
            throw new BusinessException(ErrorResult.error());
        }
        // 2.删除redis中关注的数据
        String key = Constants.FOCUS_USER + UserHolder.getUserId();
        redisTemplate.opsForSet().remove(key, unFollowId.toString());
    }
}
