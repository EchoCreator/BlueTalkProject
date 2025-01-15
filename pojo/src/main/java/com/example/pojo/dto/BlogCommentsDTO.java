package com.example.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogCommentsDTO {
    private Long blogId;
    private Long parentId;
    private Long replyId;
    private Long replyUserId;
    private String content;
}
