package com.example.server.service.impl;

import cn.hutool.json.JSONUtil;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.constant.WebSocketConstant;
import com.example.common.result.QueryRedisListResult;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.dto.ChatDTO;
import com.example.pojo.entity.Blog;
import com.example.pojo.entity.ChatContent;
import com.example.pojo.entity.ChatInfo;
import com.example.pojo.entity.Group;
import com.example.pojo.vo.*;
import com.example.server.mapper.BlogMapper;
import com.example.server.mapper.ChatMapper;
import com.example.server.service.ChatService;
import com.example.server.websocket.WebSocketServer;
import io.jsonwebtoken.Claims;
import jakarta.websocket.Session;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private BlogServiceImpl blogServiceImpl;
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Override
    public List<ChatInfoVO> getChatList() {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String key = SystemConstant.REDIS_CHAT_INFO_KEY;
        Long expiration = SystemConstant.REDIS_CHAT_INFO_EXPIRATION;
        List<ChatInfoVO> list = redisUtil.queryListWithCachePenetration(key, userId, ChatInfoVO.class, this::getChatListFromDB, null, expiration, TimeUnit.MINUTES).getData();

        return list;
    }

    @Override
    // isSingleChat=1，id是anotherUserId；否则是groupId
    public ChatVO getChatContent(Long id, Integer isSingleChat) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        List<ChatInfoVO> chatInfoVOList = getChatList();
        ChatInfoVO chatInfoVO = new ChatInfoVO();
        for (ChatInfoVO c : chatInfoVOList) {
            if (isSingleChat == 1) {
                if (c.getUserId().equals(id) && c.getGroupId() == 0) {
                    chatInfoVO = c;
                    break;
                }
            } else {
                if (c.getGroupId().equals(id)) {
                    chatInfoVO = c;
                    break;
                }
            }

        }

        String singleChatContentKey = SystemConstant.REDIS_SINGLE_CHAT_CONTENT_KEY;
        String groupChatContentKey = SystemConstant.REDIS_GROUP_CHAT_CONTENT_KEY;
        Long expiration = SystemConstant.REDIS_CHAT_CONTENT_EXPIRATION;
        List<ChatContentVO> chatContentVOList;
        if (isSingleChat == 1) {
            chatContentVOList = redisUtil.queryListWithCachePenetration(singleChatContentKey, userId, id, ChatContentVO.class, this::getChatContentFromDB, expiration, TimeUnit.MINUTES).getData();
        } else {
            chatContentVOList = redisUtil.queryListWithCachePenetration(groupChatContentKey, id, ChatContentVO.class, this::getChatContentFromDB, null, expiration, TimeUnit.MINUTES).getData();
        }

        return ChatVO.builder().chatInfo(chatInfoVO).chatContent(chatContentVOList).isSingleChat(isSingleChat).build();
    }

    @Override
    public void sendMessage(ChatDTO chatDTO) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        ChatContent chatContent = new ChatContent();
        ChatInfo chatInfo = new ChatInfo();
        BeanUtils.copyProperties(chatDTO, chatContent);
        BeanUtils.copyProperties(chatDTO, chatInfo);

        Long relationshipId = 0L;
        if (chatDTO.getIsSingleChat() == 1) {
            relationshipId = chatMapper.getRelationshipId(userId, chatDTO.getToUserId());
        }

        chatContent.setRelationshipId(relationshipId);
        chatContent.setFromUserId(userId);
        chatContent.setCreateTime(LocalDateTime.now());

        chatMapper.sendMessage(chatContent);

        // 整合chatInfo并修改redis
        String chatInfoKey = SystemConstant.REDIS_CHAT_INFO_KEY;
        Long chatInfoExpiration = SystemConstant.REDIS_CHAT_INFO_EXPIRATION;
        QueryRedisListResult<ChatInfoVO> q = redisUtil.queryListWithCachePenetration(chatInfoKey, userId, ChatInfoVO.class, this::getChatListFromDB, null, chatInfoExpiration, TimeUnit.MINUTES);
        if (!q.getFlag()) {
            ChatInfoVO c = conformityChatInfo(chatInfo, userId);
            List<ChatInfoVO> list = q.getData();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).getUserId().equals(c.getUserId())) {
                    list.set(i, c);
                    break;
                }
            }
            redisUtil.set(chatInfoKey + userId, list, chatInfoExpiration, TimeUnit.MINUTES);
        }

        // 整合chatContent并添加到redis中
        String singleChatContentKey = SystemConstant.REDIS_SINGLE_CHAT_CONTENT_KEY;
        String groupChatContentKey = SystemConstant.REDIS_GROUP_CHAT_CONTENT_KEY;
        Long chatContentExpiration = SystemConstant.REDIS_CHAT_CONTENT_EXPIRATION;
        ChatContentVO chatContentVO = new ChatContentVO();
        if (chatDTO.getIsSingleChat() == 1) {
            Long toUserId = chatDTO.getToUserId();
            QueryRedisListResult<ChatContentVO> sq = redisUtil.queryListWithCachePenetration(singleChatContentKey, userId, toUserId, ChatContentVO.class, this::getChatContentFromDB, chatContentExpiration, TimeUnit.MINUTES);
            if (!sq.getFlag()) {
                List<ChatContentVO> list = sq.getData();
                chatContentVO = conformityChatContentByVOList(chatContent, list);
                list.add(chatContentVO);
                redisUtil.set(singleChatContentKey + userId + "_" + toUserId, list, chatInfoExpiration, TimeUnit.MINUTES);
            }
        } else {
            Long groupId = chatDTO.getGroupId();
            QueryRedisListResult<ChatContentVO> gq = redisUtil.queryListWithCachePenetration(groupChatContentKey, groupId, ChatContentVO.class, this::getChatContentFromDB, null, chatContentExpiration, TimeUnit.MINUTES);
            if (!gq.getFlag()) {
                List<ChatContentVO> list = gq.getData();
                chatContentVO = conformityChatContentByVOList(chatContent, list);
                list.add(chatContentVO);
                redisUtil.set(groupChatContentKey + groupId, list, chatContentExpiration, TimeUnit.MINUTES);
            }
        }

        // 向用户或群组发送消息
        String message = JSONUtil.toJsonStr(chatContentVO);
        if (chatDTO.getIsSingleChat() == 1) {
            webSocketServer.sentMessage(message, WebSocketConstant.USER_KEY + chatDTO.getToUserId()); // 给目标用户发送消息
            webSocketServer.sentMessage(message, WebSocketConstant.USER_KEY + userId); // 给‘我’发送消息
        } else {
            webSocketServer.sentMessage(message, WebSocketConstant.GROUP_KEY + chatDTO.getGroupId()); // 给群组发送消息
        }
    }

    public List<ChatInfoVO> getChatListFromDB(Long userId) {
        List<ChatInfo> list = chatMapper.getChatList(userId);
        List<ChatInfoVO> chatInfoVOList = new ArrayList<>();
        for (ChatInfo c : list) {
            ChatInfoVO chatInfoVO = conformityChatInfo(c, userId);
            chatInfoVOList.add(chatInfoVO);
        }
        return chatInfoVOList;
    }

    public ChatInfoVO conformityChatInfo(ChatInfo c, Long userId) {
        Long anotherUserId = c.getFromUserId();
        if (c.getGroupId() == 0) {
            anotherUserId = !c.getFromUserId().equals(userId) ? c.getFromUserId() : c.getToUserId();
        }
        OtherUserVO u = userServiceImpl.getOtherUserInfo(anotherUserId);

        Group group = new Group();
        Integer memberNumber = null;
        if (c.getGroupId() != 0) {
            group = chatMapper.getGroupById(c.getGroupId());
            memberNumber = chatMapper.getGroupMemberNumber(c.getGroupId());
        }

        Blog blog = new Blog();
        if (c.getBlogId() != 0) {
            blog = blogMapper.getBlogById(c.getBlogId());
        }

        return ChatInfoVO.builder().userId(anotherUserId).username(u.getUsername()).profilePicture(u.getProfilePicture()).groupId(c.getGroupId()).groupName(group.getName()).memberNumber(memberNumber).avatar(group.getAvatar()).type(c.getType()).text(c.getText()).blogTitle(blog.getTitle()).createTime(c.getCreateTime()).build();
    }

    public List<ChatContentVO> getChatContentFromDB(Long userId, Long anotherUserId) {
        List<ChatContent> list = chatMapper.getSingleChatContentList(userId, anotherUserId);
        return conformityChatContentList(list);
    }

    public List<ChatContentVO> getChatContentFromDB(Long groupId) {
        List<ChatContent> list = chatMapper.getGroupChatContentList(groupId);
        return conformityChatContentList(list);
    }

    public List<ChatContentVO> conformityChatContentList(List<ChatContent> list) {
        List<ChatContentVO> chatContentVOList = new ArrayList<>();

        for (ChatContent c1 : list) {
            // 如果是回复信息
            ChatContentVO chatContentVO = conformityChatContent(c1, list);
            chatContentVOList.add(chatContentVO);
        }
        return chatContentVOList;
    }

    public ChatContentVO conformityChatContent(ChatContent c1, List<ChatContent> list) {
        ChatContent replyChatContent = new ChatContent();
        Blog replyBlog = new Blog();
        String replyUsername = null;
        if (c1.getReplyMsgId() != 0) {
            for (ChatContent c2 : list) {
                if (c2.getId().equals(c1.getReplyMsgId())) {
                    replyChatContent = c2;
                    OtherUserVO replyUser = userServiceImpl.getOtherUserInfo(c2.getFromUserId());
                    replyUsername = replyUser.getUsername();
                    if (replyChatContent.getBlogId() != 0) {
                        replyBlog = blogMapper.getBlogById(replyChatContent.getBlogId());
                    }
                    break;
                }
            }
        }

        // 如果是分享笔记
        Blog blog = new Blog();
        OtherUserVO blogAuthor = new OtherUserVO();
        if (c1.getBlogId() != 0) {
            blog = blogMapper.getBlogById(c1.getBlogId());
            blogAuthor = userServiceImpl.getOtherUserInfo(blog.getUserId());
        }

        OtherUserVO fromUser = userServiceImpl.getOtherUserInfo(c1.getFromUserId());

        return ChatContentVO.builder().id(c1.getId()).fromUserId(c1.getFromUserId()).toUserId(c1.getToUserId()).groupId(c1.getGroupId()).username(fromUser.getUsername()).profilePicture(fromUser.getProfilePicture()).replyMsgId(c1.getReplyMsgId()).replyMsgType(replyChatContent.getType()).replyMsgText(replyChatContent.getText()).replyMsgBlogTitle(replyBlog.getTitle()).replyMsgImage(replyChatContent.getImage()).replyUsername(replyUsername).blogId(c1.getBlogId()).blogCover(blog.getImages() == null ? null : blog.getImages().split(",")[0]).author(blogAuthor.getUsername()).title(blog.getTitle()).authorAvatar(blogAuthor.getProfilePicture()).text(c1.getText()).image(c1.getImage()).type(c1.getType()).createTime(c1.getCreateTime()).build();
    }

    public ChatContentVO conformityChatContentByVOList(ChatContent c1, List<ChatContentVO> list) {
        ChatContentVO replyChatContent = new ChatContentVO();
        Blog replyBlog = new Blog();
        String replyUsername = null;
        if (c1.getReplyMsgId() != 0) {
            for (ChatContentVO c2 : list) {
                if (c2.getId().equals(c1.getReplyMsgId())) {
                    replyChatContent = c2;
                    OtherUserVO replyUser = userServiceImpl.getOtherUserInfo(c2.getFromUserId());
                    replyUsername = replyUser.getUsername();
                    if (replyChatContent.getBlogId() != 0) {
                        replyBlog = blogMapper.getBlogById(replyChatContent.getBlogId());
                    }
                    break;
                }
            }
        }

        // 如果是分享笔记
        Blog blog = new Blog();
        OtherUserVO blogAuthor = new OtherUserVO();
        if (c1.getBlogId() != 0) {
            blog = blogMapper.getBlogById(c1.getBlogId());
            blogAuthor = userServiceImpl.getOtherUserInfo(blog.getUserId());
        }

        OtherUserVO fromUser = userServiceImpl.getOtherUserInfo(c1.getFromUserId());

        return ChatContentVO.builder().id(c1.getId()).fromUserId(c1.getFromUserId()).toUserId(c1.getToUserId()).groupId(c1.getGroupId()).username(fromUser.getUsername()).profilePicture(fromUser.getProfilePicture()).replyMsgId(c1.getReplyMsgId()).replyMsgType(replyChatContent.getType()).replyMsgText(replyChatContent.getText()).replyMsgBlogTitle(replyBlog.getTitle()).replyMsgImage(replyChatContent.getImage()).replyUsername(replyUsername).blogId(c1.getBlogId()).blogCover(blog.getImages() == null ? null : blog.getImages().split(",")[0]).author(blogAuthor.getUsername()).title(blog.getTitle()).authorAvatar(blogAuthor.getProfilePicture()).text(c1.getText()).image(c1.getImage()).type(c1.getType()).createTime(c1.getCreateTime()).build();
    }
}
