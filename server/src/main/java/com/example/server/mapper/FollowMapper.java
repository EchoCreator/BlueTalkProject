package com.example.server.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FollowMapper {
    @Select("select follow_user_id from follow where user_id=#{userId} order by create_time desc")
    List<Long> getFollowee(Long userId);

    @Select("select user_id from follow where follow_user_id=#{userId} order by create_time desc")
    List<Long> getFans(Long userId);

    @Select("select count(*) from follow where user_id=#{userId} and follow_user_id=#{followUserId}")
    Integer isFollowed(Long userId, Long followUserId);

    @Insert("insert into follow(user_id, follow_user_id, create_time) values (#{userId},#{followUserId},now())")
    void followUser(Long userId, Long followUserId);

    @Delete("delete from follow where user_id=#{userId} and follow_user_id=#{followUserId}")
    void cancelFollowUser(Long userId, Long followUserId);
}
