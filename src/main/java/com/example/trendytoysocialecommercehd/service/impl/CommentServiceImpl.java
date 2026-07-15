package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.trendytoysocialecommercehd.entity.Comment;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.CommentMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.CommentService;
import com.example.trendytoysocialecommercehd.service.SocialActivityService;
import com.example.trendytoysocialecommercehd.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private SocialActivityService socialActivityService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Override
    public IPage<Comment> getCommentsByActivityId(Page<Comment> page, String activityId, String keyword) {
        // 查询所有状态的评论，不只是审核通过的
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getActivityId, activityId)
                .isNull(Comment::getParentCommentId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(Comment::getContent, keyword.trim());
        }
        wrapper.orderByDesc(Comment::getCommentedAt);

        IPage<Comment> resultPage = this.page(page, wrapper);

        List<Comment> comments = resultPage.getRecords();
        if (!comments.isEmpty()) {
            List<String> rootCommentIds = comments.stream()
                    .map(Comment::getCommentId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.in(Comment::getRootCommentId, rootCommentIds)
                    .orderByAsc(Comment::getCommentedAt);

            List<Comment> replies = this.list(replyWrapper);

            Map<String, List<Comment>> repliesMap = replies.stream()
                    .collect(Collectors.groupingBy(Comment::getRootCommentId));

            List<String> allUserIds = new ArrayList<>();
            comments.forEach(c -> {
                if (c.getUserId() != null) allUserIds.add(c.getUserId());
            });
            replies.forEach(c -> {
                if (c.getUserId() != null) allUserIds.add(c.getUserId());
                if (c.getParentCommentId() != null) {
                    Comment parentComment = replies.stream()
                            .filter(r -> r.getCommentId().equals(c.getParentCommentId()))
                            .findFirst()
                            .orElse(null);
                    if (parentComment != null && parentComment.getUserId() != null) {
                        allUserIds.add(parentComment.getUserId());
                    }
                }
            });

            if (!allUserIds.isEmpty()) {
                List<String> distinctUserIds = allUserIds.stream().distinct().collect(Collectors.toList());
                Map<String, User> userMap = userMapper.selectBatchIds(distinctUserIds)
                        .stream()
                        .collect(Collectors.toMap(User::getUserId, u -> u));

                comments.forEach(comment -> {
                    User user = userMap.get(comment.getUserId());
                    if (user != null) {
                        Comment.UserInfo userInfo = new Comment.UserInfo();
                        userInfo.setUserId(user.getUserId());
                        userInfo.setUsername(user.getUsername());
                        userInfo.setAvatarUrl(user.getAvatarUrl());
                        comment.setUserInfo(userInfo);
                    }
                });

                replies.forEach(comment -> {
                    User user = userMap.get(comment.getUserId());
                    if (user != null) {
                        Comment.UserInfo userInfo = new Comment.UserInfo();
                        userInfo.setUserId(user.getUserId());
                        userInfo.setUsername(user.getUsername());
                        userInfo.setAvatarUrl(user.getAvatarUrl());
                        comment.setUserInfo(userInfo);
                    }

                    if (comment.getParentCommentId() != null) {
                        Comment parentComment = comments.stream()
                                .filter(c -> c.getCommentId().equals(comment.getParentCommentId()))
                                .findFirst()
                                .orElse(null);
                        if (parentComment == null) {
                            parentComment = replies.stream()
                                    .filter(r -> r.getCommentId().equals(comment.getParentCommentId()))
                                    .findFirst()
                                    .orElse(null);
                        }
                        if (parentComment != null && parentComment.getUserId() != null) {
                            User replyToUser = userMap.get(parentComment.getUserId());
                            if (replyToUser != null) {
                                Comment.UserInfo replyToUserInfo = new Comment.UserInfo();
                                replyToUserInfo.setUserId(replyToUser.getUserId());
                                replyToUserInfo.setUsername(replyToUser.getUsername());
                                replyToUserInfo.setAvatarUrl(replyToUser.getAvatarUrl());
                                comment.setReplyToUserInfo(replyToUserInfo);
                            }
                        }
                    }
                });
            }

            comments.forEach(comment -> {
                comment.setReplies(repliesMap.getOrDefault(comment.getCommentId(), new ArrayList<>()));
            });
        }

        return resultPage;
    }

    @Override
    @Transactional
    public Comment createComment(Comment comment) {
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        // 为了方便调试，直接设置为审核通过
        comment.setAuditStatus("审核通过");
        comment.setCommentedAt(new Date());

        // 确保 userId 被正确保存
        if (comment.getUserId() == null || comment.getUserId().isEmpty()) {
            // 这里可以从JWT token中获取当前登录用户的ID
            // 暂时先用默认值，实际应该从SecurityContext获取
            comment.setUserId("anonymous");
        }

        this.save(comment);

        if (comment.getParentCommentId() != null) {
            String rootCommentId = comment.getRootCommentId() != null
                    ? comment.getRootCommentId()
                    : comment.getParentCommentId();
            Comment parent = this.getById(rootCommentId);
            if (parent != null) {
                parent.setReplyCount(parent.getReplyCount() + 1);
                this.updateById(parent);
            }
        }

        socialActivityService.incrementCommentCount(comment.getActivityId());

        // 加载用户信息返回
        if (comment.getUserId() != null) {
            User user = userMapper.selectById(comment.getUserId());
            if (user != null) {
                Comment.UserInfo userInfo = new Comment.UserInfo();
                userInfo.setUserId(user.getUserId());
                userInfo.setUsername(user.getUsername());
                userInfo.setAvatarUrl(user.getAvatarUrl());
                comment.setUserInfo(userInfo);
            }
        }

        return comment;
    }

    @Override
    @Transactional
    public boolean likeComment(String commentId) {
        Comment comment = this.getById(commentId);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            return this.updateById(comment);
        }
        return false;
    }

    @Override
    @Transactional
    public boolean unlikeComment(String commentId) {
        Comment comment = this.getById(commentId);
        if (comment != null && comment.getLikeCount() > 0) {
            comment.setLikeCount(comment.getLikeCount() - 1);
            return this.updateById(comment);
        }
        return false;
    }
}