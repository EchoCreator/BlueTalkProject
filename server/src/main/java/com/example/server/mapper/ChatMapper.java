package com.example.server.mapper;

import com.example.pojo.entity.ChatContent;
import com.example.pojo.entity.ChatInfo;
import com.example.pojo.entity.Group;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMapper {
    List<ChatInfo> getChatList(Long userId);

    @Select("select * from `group` where id=#{groupId}")
    Group getGroupById(Long groupId);

    @Select("select * from chat where (from_user_id=#{userId} and to_user_id=#{anotherUserId}) or" +
            " (from_user_id=#{anotherUserId} and to_user_id=#{userId})")
    List<ChatContent> getSingleChatContentList(Long userId, Long anotherUserId);

    @Select("select * from chat where group_id=#{groupId}")
    List<ChatContent> getGroupChatContentList(Long groupId);

    @Select("select member_number from `group` where id=#{groupId}")
    Integer getGroupMemberNumber(Long groupId);

    @Select("select id from single_chat_ralationship where (user_id=#{myUserId} and another_user_id=#{anotherUserId}) or" +
            " (user_id=#{anotherUserId} and another_user_id=#{myUserId})")
    Long getRelationshipId(Long myUserId, Long anotherUserId);

    void sendMessage(ChatContent chatContent);
}
