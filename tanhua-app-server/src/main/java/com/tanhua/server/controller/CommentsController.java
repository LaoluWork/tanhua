package com.tanhua.server.controller;

import com.tanhua.model.vo.PageResult;
import com.tanhua.server.service.CommentsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-03 11:28
 */
@RestController
@RequestMapping("/comments")
public class CommentsController {

    @Autowired
    private CommentsService commentsService;

    /**
     * 发布评论
     */
    @PostMapping
    public ResponseEntity saveComments(@RequestBody Map map) {
        String movementId = (String) map.get("movementId");
        String comment = (String) map.get("comment");
        commentsService.saveComments(movementId, comment);
        return ResponseEntity.ok(null);

    }

    /**
     * 分页查询评论列表
     */
    @GetMapping
    public ResponseEntity findComments(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer pagesize,
                                       String movementId) {
        PageResult pr = commentsService.findComments(movementId, page, pagesize);
        return ResponseEntity.ok(pr);

    }

    /**
     * 点赞评论
     */
    @GetMapping("/{id}/like")
    public ResponseEntity like(@PathVariable("id") String id) {
        Integer count = commentsService.likeTheComment(id);
        return ResponseEntity.ok(count);
    }
    /**
     * 取消点赞评论
     */
    @GetMapping("/{id}/dislike")
    public ResponseEntity dislike(@PathVariable("id") String id) {
        Integer count = commentsService.dislikeTheComment(id);
        return ResponseEntity.ok(count);
    }


}
