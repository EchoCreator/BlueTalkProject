package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatContentVO implements Serializable {
    private Long id;

    private Long fromUserId;
    private Long toUserId;
    private Long groupId;

    // 发送消息（fromUserId）的用户名和头像（用于群聊）
    private String username;
    private String profilePicture;

    // 如果是回复消息
    private Long replyMsgId;
    private Integer replyMsgType;
    private String replyMsgText;
    private String replyMsgBlogTitle;
    private String replyMsgImage;
    private String replyUsername;

    private Long blogId;
    private String blogCover;
    private String author;
    private String title;
    private String authorAvatar;

    private String text;
    private String image;
    private Integer type;

    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createTime;
}
