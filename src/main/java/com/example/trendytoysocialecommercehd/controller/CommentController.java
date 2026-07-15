package com.example.trendytoysocialecommercehd.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.Comment;
import com.example.trendytoysocialecommercehd.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/activity/{activityId}/comments")
    public Result<IPage<Comment>> getComments(
            @PathVariable String activityId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer size,
            @RequestParam(required = false) String keyword) {
        Page<Comment> pageParam = new Page<>(page, size);
        IPage<Comment> result = commentService.getCommentsByActivityId(pageParam, activityId, keyword);
        return Result.success(result);
    }

    @GetMapping("/comment/{commentId}")
    public Result<Comment> getCommentDetail(@PathVariable String commentId) {
        Comment comment = commentService.getById(commentId);
        return Result.success(comment);
    }

    @PostMapping("/comment")
    public Result<Comment> createComment(@RequestBody Comment comment) {
        Comment created = commentService.createComment(comment);
        return Result.success(created);
    }

    @PutMapping("/comment")
    public Result<Comment> updateComment(@RequestBody Comment comment) {
        commentService.updateById(comment);
        return Result.success(commentService.getById(comment.getCommentId()));
    }

    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable String commentId) {
        commentService.removeById(commentId);
        return Result.success();
    }

    @PostMapping("/comment/{commentId}/like")
    public Result<Void> likeComment(@PathVariable String commentId) {
        commentService.likeComment(commentId);
        return Result.success();
    }

    @DeleteMapping("/comment/{commentId}/like")
    public Result<Void> unlikeComment(@PathVariable String commentId) {
        commentService.unlikeComment(commentId);
        return Result.success();
    }
}