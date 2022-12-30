package com.tanhua.server.controller;

import com.tanhua.model.enums.CommentType;
import com.tanhua.model.vo.PageResult;
import com.tanhua.model.vo.UserInfoVo;
import com.tanhua.server.service.MessagesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-04 15:18
 */
@RestController
@RequestMapping("/messages")
public class MessagesController {

    @Autowired
    private MessagesService messagesService;

    /**
     * 根据环信用户id，查询用户详情
     */
    @GetMapping("/userinfo")
    public ResponseEntity userinfo(String huanxinId) {
        UserInfoVo vo = messagesService.findUserInfoByHuanxin(huanxinId);
        return ResponseEntity.ok(vo);
    }
    /**
     * 确认添加好友
     */
    @PostMapping("/contacts")
    public ResponseEntity contacts(@RequestBody Map map) {
        Long friendId = Long.valueOf(map.get("userId").toString());
        messagesService.contacts(friendId);
        return ResponseEntity.ok(null);
    }

    /**
     * 查询联系人列表，可按照用户昵称关键字
     */
    @GetMapping("/contacts")
    public ResponseEntity contacts( @RequestParam(defaultValue = "1") Integer page,
                                     @RequestParam(defaultValue = "10") Integer pagesize,
                                    String keyword) {
        PageResult pr = messagesService.findFriends(page, pagesize, keyword);
        return ResponseEntity.ok(pr);
    }

    /**
     * 查询点赞自己动态的简要情况列表
     */
    @GetMapping("/likes")
    public ResponseEntity likes( @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer pagesize) {
        PageResult pr = messagesService.findInteraction(page, pagesize, CommentType.LIKE);
        return ResponseEntity.ok(pr);
    }
    /**
     * 查询喜欢自己动态的简要情况列表
     */
    @GetMapping("/loves")
    public ResponseEntity loves( @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer pagesize) {
        PageResult pr = messagesService.findInteraction(page, pagesize, CommentType.LOVE);
        return ResponseEntity.ok(pr);
    }
    /**
     * 查询评论自己动态的简要情况列表
     */
    @GetMapping("/comments")
    public ResponseEntity comments( @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer pagesize) {
        PageResult pr = messagesService.findInteraction(page, pagesize, CommentType.COMMENT);
        return ResponseEntity.ok(pr);
    }
}
