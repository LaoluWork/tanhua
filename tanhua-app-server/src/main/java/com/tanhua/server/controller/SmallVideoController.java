package com.tanhua.server.controller;

import com.tanhua.model.vo.PageResult;
import com.tanhua.server.service.SmallVideosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/smallVideos")
public class SmallVideoController {

    @Autowired
    private SmallVideosService videosService;

    /**
     * 发布小视频
     *  请求参数：
     *      videoThumbnail: 封面图片
     *      videofile： 视频文件
     */
    @PostMapping
    public ResponseEntity saveVideos(MultipartFile videoThumbnail, MultipartFile videoFile) throws IOException {
        videosService.saveVideos(videoThumbnail, videoFile);
        return ResponseEntity.ok(null);
    }

    /**
     * 查询视频列表
     */
    @GetMapping
    public ResponseEntity queryVideoList(@RequestParam(defaultValue = "1")  Integer page,
                                         @RequestParam(defaultValue = "10") Integer pagesize) {
        PageResult result = videosService.queryVideoList(page, pagesize);
        return ResponseEntity.ok(result);
    }

    /**
     *小视频里关注用户
     */
    @PostMapping("{uid}/userFocus")
    public ResponseEntity userFocus(@PathVariable("uid") Long uid) {
        videosService.focusUser(uid);
        return ResponseEntity.ok(null);
    }

    /**
     * 小视频里取消关注用户
     */
    @PostMapping("/{uid}/userUnFocus")
    public ResponseEntity userUnFocus(@PathVariable("uid") Long uid) {
        videosService.userUnFocus(uid);
        return ResponseEntity.ok(null);
    }

}