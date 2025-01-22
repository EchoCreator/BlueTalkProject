package com.example.server.service;

import com.example.pojo.vo.ChatInfoVO;
import com.example.pojo.vo.ChatVO;

import java.util.List;

public interface ChatService {
    List<ChatInfoVO> getChatList();

    ChatVO getChatContent(Long id,Integer isSingleChat);
}
