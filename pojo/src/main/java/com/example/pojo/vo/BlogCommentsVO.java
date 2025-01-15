package com.example.pojo.vo;

import com.example.pojo.entity.BlogComments;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogCommentsVO implements Serializable {
    private Long id;
    private Long blogId;
    private Long userId;
    private Long parentId;
    private Long replyId;
    private Long replyUserId;
    private String username;
    private String profilePicture;
    private String content;
    private Integer likes;
    private Integer status;
    private String replyUsername; // 回复的评论用户名，如果回复的是一级评论或者帖子则为null
    private List<BlogCommentsVO> childrenComments;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime updateTime;
}
