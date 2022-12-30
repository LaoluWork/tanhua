package com.tanhua.dubbo.api;

import com.tanhua.model.enums.CommentType;
import com.tanhua.model.mongo.Comment;
import com.tanhua.model.vo.PageResult;

import java.util.List;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-03 11:25
 */
public interface CommentApi {

    //判断是否该类型的互动数据
    Boolean hasComment(String movementId, Long userId, CommentType like);

    //保存互动（点赞、喜欢、评论），并获取该类型互动的数量
    Integer save(Comment comment1);

    // 分页查询评论列表
    List<Comment> findComments(String movementId, CommentType commentType, Integer page, Integer pagesize);

    //删除互动数据
    Integer delete(Comment comment);

    //判断是否已点赞评论
    Boolean hasTheComment(String commentId, CommentType comment);

    //将对应的评论点赞数加1
    Integer likeTheComment(String commentId);

    //将对应的评论点赞数减1
    Integer dislikeComment(String commentId);

    // 根据pulishUserId查询点赞列表
    PageResult findByPublishUserId(Integer page, Integer pagesize, Long userId, CommentType type);
}
