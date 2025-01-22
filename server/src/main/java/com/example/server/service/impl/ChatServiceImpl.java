package com.example.server.service.impl;

import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.entity.Blog;
import com.example.pojo.entity.ChatContent;
import com.example.pojo.entity.ChatInfo;
import com.example.pojo.entity.Group;
import com.example.pojo.vo.*;
import com.example.server.mapper.BlogMapper;
import com.example.server.mapper.ChatMapper;
import com.example.server.service.ChatService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public List<ChatInfoVO> getChatListFromDB(Long userId) {
        List<ChatInfo> list = chatMapper.getChatList(userId);
        List<ChatInfoVO> chatInfoVOList = new ArrayList<>();
        for (ChatInfo c : list) {
            Long anotherUserId = c.getFromUserId();
            if (c.getGroupId() == 0) {
                anotherUserId = !c.getFromUserId().equals(userId) ? c.getFromUserId() : c.getToUserId();

            }
            OtherUserVO u = userServiceImpl.getOtherUserInfo(anotherUserId);

            Group group = new Group();
            if (c.getGroupId() != 0) {
                group = chatMapper.getGroupById(c.getGroupId());
            }

            Blog blog = new Blog();
            if (c.getBlogId() != 0) {
                blog = blogMapper.getBlogById(c.getBlogId());
            }

            ChatInfoVO chatInfoVO = ChatInfoVO.builder()
                    .userId(anotherUserId)
                    .username(u.getUsername())
                    .profilePicture(u.getProfilePicture())
                    .groupId(c.getGroupId())
                    .groupName(group.getName())
                    .avatar(group.getAvatar())
                    .type(c.getType())
                    .text(c.getText())
                    .blogTitle(blog.getTitle())
                    .createTime(c.getCreateTime())
                    .build();
            chatInfoVOList.add(chatInfoVO);
        }
        return chatInfoVOList;
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
            ChatContent replyChatContent = new ChatContent();
            Blog replyBlog = new Blog();
            if (c1.getReplyMsgId() != 0) {
                for (ChatContent c2 : list) {
                    if (c2.getId().equals(c1.getReplyMsgId())) {
                        replyChatContent = c2;
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

            // 如果是群聊
            OtherUserVO fromUser = new OtherUserVO();
            if (c1.getGroupId() != 0) {
                fromUser = userServiceImpl.getOtherUserInfo(c1.getFromUserId());
            }

            ChatContentVO chatContentVO = ChatContentVO.builder()
                    .id(c1.getId())
                    .fromUserId(c1.getFromUserId())
                    .toUserId(c1.getToUserId())
                    .groupId(c1.getGroupId())
                    .username(fromUser.getUsername())
                    .profilePicture(fromUser.getProfilePicture())
                    .replyMsgId(c1.getReplyMsgId())
                    .replyMsgType(replyChatContent.getType())
                    .replyMsgText(replyChatContent.getText())
                    .replyMsgBlogTitle(replyBlog.getTitle())
                    .blogId(c1.getBlogId())
                    .blogCover(blog.getImages() == null ? null : blog.getImages().split("")[0])
                    .author(blogAuthor.getUsername())
                    .title(blog.getTitle())
                    .authorAvatar(blogAuthor.getProfilePicture())
                    .text(c1.getText())
                    .image(c1.getImage())
                    .type(c1.getType())
                    .createTime(c1.getCreateTime())
                    .build();
            chatContentVOList.add(chatContentVO);
        }
        return chatContentVOList;
    }
}
