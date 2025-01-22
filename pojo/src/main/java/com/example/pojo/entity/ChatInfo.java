package com.example.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
// 消息列表中展示的内容
public class ChatInfo {
    private Long fromUserId;
    private Long toUserId;
    private Long groupId;
    private Long blogId;
    private String text;
    private String image;
    private Integer type;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createTime;
}
