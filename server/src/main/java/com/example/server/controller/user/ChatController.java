package com.example.server.controller.user;

import com.example.common.result.Result;
import com.example.pojo.dto.ChatDTO;
import com.example.pojo.vo.ChatContentVO;
import com.example.pojo.vo.ChatInfoVO;
import com.example.pojo.vo.ChatVO;
import com.example.server.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @GetMapping("/getChatList")
    public Result<List<ChatInfoVO>> getChatList() {
        List<ChatInfoVO> list = chatService.getChatList();
        return Result.success(list);
    }

    @GetMapping("/getChatContent")
    public Result<ChatVO> getChatContent(Long id,Integer isSingleChat) {
        ChatVO chatVO=chatService.getChatContent(id,isSingleChat);
        return Result.success(chatVO);
    }

    @PutMapping("/sendMessage")
    public Result sendMessage(ChatDTO chatDTO) {
        chatService.sendMessage(chatDTO);
        return Result.success();
    }
}
