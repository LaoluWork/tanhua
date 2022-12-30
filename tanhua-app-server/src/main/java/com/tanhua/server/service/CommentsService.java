package com.tanhua.server.service;

import cn.hutool.core.collection.CollUtil;
import com.tanhua.commons.utils.Constants;
import com.tanhua.dubbo.api.CommentApi;
import com.tanhua.dubbo.api.UserInfoApi;
import com.tanhua.model.domain.UserInfo;
import com.tanhua.model.enums.CommentType;
import com.tanhua.model.mongo.Comment;
import com.tanhua.model.vo.CommentVo;
import com.tanhua.model.vo.ErrorResult;
import com.tanhua.model.vo.PageResult;
import com.tanhua.server.exception.BusinessException;
import com.tanhua.server.interceptor.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-03 11:29
 */
@Service
@Slf4j
public class CommentsService {

    @DubboReference
    private CommentApi commentApi;

    @DubboReference
    private UserInfoApi userInfoApi;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private  MqMessageService mqMessageService;

    //发布评论
    public void saveComments(String movementId, String comment) {
        //1.获取用户id
        Long userId = UserHolder.getUserId();
        //2.构建Comment对象
        Comment comment1 = new Comment();
        comment1.setPublishId(new ObjectId(movementId));
        comment1.setCommentType(CommentType.COMMENT.getType());
        comment1.setContent(comment);
        comment1.setUserId(userId);
        comment1.setCreated(System.currentTimeMillis());
        //3.调用api保存评论
        commentApi.save(comment1);

        // 向RabbitMQ发送消息，记录行为
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0205", "movement", movementId);
    }

    // 分页查询评论列表
    public PageResult findComments(String movementId, Integer page, Integer pagesize) {
        //1.调用api查询评论列表
        List<Comment> list = commentApi.findComments(movementId, CommentType.COMMENT, page, pagesize);
        //2.判断列表是否为空
        if (CollUtil.isEmpty(list)) {
            return new PageResult();
        }
        //3.提取所有用户id，调用userinfoApi查询用户详情
        List<Long> ids = CollUtil.getFieldValues(list, "userId", Long.class);
        Map<Long, UserInfo> map = userInfoApi.findByIds(ids, null);
        //4.构建vo对象
        List<CommentVo> vos = new ArrayList<>();
        for (Comment comment : list) {
            UserInfo userInfo = map.get(comment.getUserId());
            if (userInfo != null) {
                CommentVo vo = CommentVo.init(userInfo, comment);
                String key = Constants.COMMENT_INTERACT_KEY + comment.getId();
                String hashKey = Constants.COMMENT_LIKE_HASHKEY+ UserHolder.getUserId();
                // 判断动态里的评论，自己是否已经点赞
                if(redisTemplate.opsForHash().hasKey(key, hashKey)){
                    vo.setHasLiked(1);
                }
                vos.add(vo);
            }

        }
        //5.构造返回值
        return new PageResult(page, pagesize, 0l, vos);
    }

    // 点赞动态
    public Integer likeComment(String movementId) {
        //1.调用api查询用户是否已经已点赞
        Boolean hasComment = commentApi.hasComment(movementId, UserHolder.getUserId(), CommentType.LIKE);
        //2.如果已点赞，抛出异常
        if (hasComment) {
            throw new BusinessException(ErrorResult.likeError());
        }
        //3.调用api保存Comment数据到MongoDB，同时修改对应动态的点赞数
        Comment comment = new Comment();
        comment.setPublishId(new ObjectId(movementId));
        comment.setCommentType(CommentType.LIKE.getType());
        comment.setUserId(UserHolder.getUserId());
        comment.setCreated(System.currentTimeMillis());
        Integer count = commentApi.save(comment);
        //4.拼接redis的key，将用户的点赞状态存入redis中
        String key = Constants.MOVEMENTS_INTERACT_KEY + movementId;
        String hashKey = Constants.MOVEMENT_LIKE_HASHKEY + UserHolder.getUserId();
        redisTemplate.opsForHash().put(key, hashKey, "1");

        // 向RabbitMQ发送消息，记录行为
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0203", "movement", movementId);

        return count;
    }

    // 取消点赞
    public Integer dislikeComment(String movementId) {
        //1.查询是否存在对应Comment，判断是否已点赞
        Boolean hasComment = commentApi.hasComment(movementId, UserHolder.getUserId(), CommentType.LIKE);
        //2.如果未点赞，抛出异常
        if (!hasComment) {
            throw new BusinessException(ErrorResult.disLikeError());
        }
        //3.删除Comment数据，并同步修改对应动态表中的数据的点赞数
        Comment comment = new Comment();
        comment.setPublishId(new ObjectId(movementId));
        comment.setCommentType(CommentType.LIKE.getType());
        comment.setUserId(UserHolder.getUserId());
        Integer count = commentApi.delete(comment);
        //4.删除redis中点赞状态数据
        String key = Constants.MOVEMENTS_INTERACT_KEY + movementId;
        String hashKey = Constants.MOVEMENT_LIKE_HASHKEY + UserHolder.getUserId();
        redisTemplate.opsForHash().delete(key, hashKey);

        // 向RabbitMQ发送消息，记录行为
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0206", "movement", movementId);

        return count;

    }

    //喜欢
    public Integer loveComment(String movementId) {
        //1.调用api查询用户是否已经已喜欢
        Boolean hasComment = commentApi.hasComment(movementId, UserHolder.getUserId(), CommentType.LOVE);
        //2.如果已点赞，抛出异常
        if (hasComment) {
            throw new BusinessException(ErrorResult.loveError());
        }
        //3.调用api保存Comment数据到MongoDB
        Comment comment = new Comment();
        comment.setPublishId(new ObjectId(movementId));
        comment.setCommentType(CommentType.LOVE.getType());
        comment.setUserId(UserHolder.getUserId());
        comment.setCreated(System.currentTimeMillis());
        Integer count = commentApi.save(comment);
        //4.拼接redis的key，将用户的点赞状态存入redis中
        String key = Constants.MOVEMENTS_INTERACT_KEY + movementId;
        String hashKey = Constants.MOVEMENT_LOVE_HASHKEY + UserHolder.getUserId();
        redisTemplate.opsForHash().put(key, hashKey, "1");

        // 向RabbitMQ发送消息，记录行为
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0204", "movement", movementId);

        return count;

    }

    //取消喜欢
    public Integer unloveComment(String movementId) {
        //1.查询是否存在对应Comment，判断是否已喜欢
        Boolean hasComment = commentApi.hasComment(movementId, UserHolder.getUserId(), CommentType.LOVE);
        //2.如果未点赞，抛出异常
        if (!hasComment) {
            throw new BusinessException(ErrorResult.disloveError());
        }
        //3.删除Comment数据
        Comment comment = new Comment();
        comment.setPublishId(new ObjectId(movementId));
        comment.setCommentType(CommentType.LOVE.getType());
        comment.setUserId(UserHolder.getUserId());
        Integer count = commentApi.delete(comment);
        //4.删除redis中点赞状态数据
        String key = Constants.MOVEMENTS_INTERACT_KEY + movementId;
        String hashKey = Constants.MOVEMENT_LOVE_HASHKEY + UserHolder.getUserId();
        redisTemplate.opsForHash().delete(key, hashKey);

        // 向RabbitMQ发送消息，记录行为
        mqMessageService.sendLogMessage(UserHolder.getUserId(), "0207", "movement", movementId);

        return count;
    }

    //点赞动态里别人的评论内容
    public Integer likeTheComment(String commentId) {
        //1.查询出对应的comment，判断是否存在对应评论
        Boolean hasTheComment = commentApi.hasTheComment(commentId, CommentType.COMMENT);
        //2.不存在就抛出异常
        if (!hasTheComment) {
            throw new BusinessException(ErrorResult.likeCommentError());
        }
        //3.存在就设置comment的likeCount+1
        Integer count = commentApi.likeTheComment(commentId);
        //4.redis中存入对应信息，显示自己是否已经点赞该评论
        String key = Constants.COMMENT_INTERACT_KEY + commentId;
        String hashKey = Constants.COMMENT_LIKE_HASHKEY+ UserHolder.getUserId();
        redisTemplate.opsForHash().put(key, hashKey, "1");
        //5.返回评论数值
        return count;
    }

    //取消点赞评论
    public Integer dislikeTheComment(String commentId) {
        //1.查询出对应的comment，判断是否存在对应评论
        Boolean hasTheComment = commentApi.hasTheComment(commentId, CommentType.COMMENT);
        //2.不存在就抛出异常
        if (!hasTheComment) {
            throw new BusinessException(ErrorResult.likeCommentError());
        }
        //3.存在就去数据库设置likeCount-1
        Integer count = commentApi.dislikeComment(commentId);
        //4.删除redis中信息
        String key = Constants.COMMENT_INTERACT_KEY + commentId;
        String hashKey = Constants.COMMENT_LIKE_HASHKEY+ UserHolder.getUserId();
        redisTemplate.opsForHash().delete(key, hashKey);

        return count;
    }
}
