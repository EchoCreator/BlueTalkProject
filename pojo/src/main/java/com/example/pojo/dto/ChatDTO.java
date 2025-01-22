package com.example.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {
    private Long toUserId;
    private Long groupId;
    private Integer isSingleChat; // 单聊或群聊
    private Integer type; // 消息类型：本文，图片，笔记
    private String text;
    private String image;
    private Long blogId;
    private Long replyMsgId; // 如果是回复消息
}
