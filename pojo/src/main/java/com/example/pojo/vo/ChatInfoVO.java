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
public class ChatInfoVO implements Serializable {
    // 与‘我’聊天的用户id
    private Long userId;
    private String username;
    private String profilePicture;

    // ‘我’参与的群聊id
    private Long groupId;
    private String groupName;
    private Integer memberNumber;
    private String avatar;

    private Integer type;
    // 如果最新消息是文本
    private String text;
    // 如果最新消息是笔记
    private String blogTitle;

    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createTime;
}
