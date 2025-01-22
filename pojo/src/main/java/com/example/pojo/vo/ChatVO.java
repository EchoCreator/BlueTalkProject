package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatVO implements Serializable {
    private ChatInfoVO chatInfo;
    private List<ChatContentVO> chatContent;
    private Integer isSingleChat;
}
