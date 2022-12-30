package com.tanhua.dubbo.api;

import com.tanhua.model.enums.CommentType;
import com.tanhua.model.mongo.Comment;
import com.tanhua.model.mongo.Movement;
import com.tanhua.model.vo.PageResult;
import org.apache.dubbo.config.annotation.DubboService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;


/**
 * @author LaoLu
 * @Description:
 * @create 2022-09-03 11:25
 */
@DubboService
public class CommentApiImpl implements CommentApi {

    @Autowired
    private MongoTemplate mongoTemplate;



    //发表评论，并获取评论数量
    @Override
    public Integer save(Comment comment1) {
        //1.查询动态
        Movement movement = mongoTemplate.findById(comment1.getPublishId(), Movement.class);
        //2.向comment对象设置被评论人的id
        if (movement != null) {
            comment1.setPublishUserId(movement.getUserId());
        }
        //3.将comment保存到数据库
        mongoTemplate.save(comment1);
        //4.更新动态中对应的字段
        Query query = Query.query(Criteria.where("id").is(comment1.getPublishId()));
        Update update = new Update();
        if (comment1.getCommentType() == CommentType.LIKE.getType()) {
            update.inc("likeCount", 1);
        } else if (comment1.getCommentType() == CommentType.COMMENT.getType()) {
            update.inc("commentCount", 1);
        } else {
            update.inc("loveCount", 1);
        }
        //设置更新参数
        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true);
        Movement modify = mongoTemplate.findAndModify(query, update, options, Movement.class);
        //5.获取最新的评论数量并返回
        return modify.statisCount(comment1.getCommentType());

    }

    //分页查询评论列表
    @Override
    public List<Comment> findComments(String movementId, CommentType commentType, Integer page, Integer pagesize) {
        //1.构造查询条件
        Query query = Query.query(Criteria.where("publishId").is(new ObjectId(movementId))
                        .and("commentType").is(commentType.getType()))
                .skip((page - 1) * pagesize)
                .limit(pagesize)
                .with(Sort.by(Sort.Order.desc("created")));
        //2.查询并返回
        return mongoTemplate.find(query, Comment.class);
    }

    //判断comment数据是否存在
    @Override
    public Boolean hasComment(String movementId, Long userId, CommentType like) {
        Criteria criteria = Criteria.where("publishId").is(new ObjectId(movementId))
                .and("userId").is(userId)
                .and("commentType").is(like.getType());
        Query query = Query.query(criteria);
        return mongoTemplate.exists(query, Comment.class);
    }

    //删除互动数据
    @Override
    public Integer delete(Comment comment) {
        //1.删除Comment表数据
        Criteria criteria = Criteria.where("publishId").is(comment.getPublishId())
                .and("userId").is(comment.getUserId())
                .and("commentType").is(comment.getCommentType());
        Query query = Query.query(criteria);
        mongoTemplate.remove(query, Comment.class);
        //2.修改动态表中的总数量
        Query movementQuery = Query.query(Criteria.where("id").is(comment.getPublishId()));
        Update update = new Update();
        if (comment.getCommentType() == CommentType.LIKE.getType()) {
            update.inc("likeCount", -1);
        } else if (comment.getCommentType() == CommentType.COMMENT.getType()) {
            update.inc("commentCount", -1);
        } else {
            update.inc("loveCount", -1);
        }
        //设置更新参数
        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true);
        Movement modify = mongoTemplate.findAndModify(movementQuery, update, options, Movement.class);
        //5.获取最新的评论数量并返回
        return modify.statisCount(comment.getCommentType());
    }

    // 判断是否存在某指定评论
    @Override
    public Boolean hasTheComment(String commentId, CommentType comment) {
        //根据commentId和comment类型来判断是否存在数据
        Query query = Query.query(Criteria.where("id").is(new ObjectId(commentId))
                .and("commentType").is(comment.getType()));
        return mongoTemplate.exists(query, Comment.class);
    }

    //将指定动态里的评论点赞数加1
    @Override
    public Integer likeTheComment(String commentId) {
        Query query = Query.query(Criteria.where("id").is(new ObjectId(commentId)));
        Update update = new Update();
        update.inc("likeCount", 1);
        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true);
        Comment modify = mongoTemplate.findAndModify(query, update, options, Comment.class);
        return modify.getLikeCount();
    }

    //将指定动态里的评论点赞数减1
    @Override
    public Integer dislikeComment(String commentId) {
        Query query = Query.query(Criteria.where("id").is(new ObjectId(commentId)));
        Update update = new Update();
        update.inc("likeCount", -1);
        FindAndModifyOptions options = new FindAndModifyOptions();
        options.returnNew(true);
        Comment modify = mongoTemplate.findAndModify(query, update, options, Comment.class);
        return modify.getLikeCount();
    }

    // 根据pulishUserId查询点赞列表
    @Override
    public PageResult findByPublishUserId(Integer page, Integer pagesize, Long userId, CommentType type) {
        Query query = Query.query(Criteria.where("publishUserId").is(userId).and("commentType").is(type.getType()));
        Long count = mongoTemplate.count(query, Comment.class);

        query.skip((page - 1) * pagesize).limit(pagesize).with(Sort.by(Sort.Order.desc("created")));
        List<Comment> list = mongoTemplate.find(query, Comment.class);

        return new PageResult(page, pagesize, count, list);

    }
}
