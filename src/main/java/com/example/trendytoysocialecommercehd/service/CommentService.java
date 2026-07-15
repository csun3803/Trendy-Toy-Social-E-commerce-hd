package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.trendytoysocialecommercehd.entity.Comment;

public interface CommentService extends IService<Comment> {
    IPage<Comment> getCommentsByActivityId(Page<Comment> page, String activityId, String keyword);
    Comment createComment(Comment comment);
    boolean likeComment(String commentId);
    boolean unlikeComment(String commentId);
}