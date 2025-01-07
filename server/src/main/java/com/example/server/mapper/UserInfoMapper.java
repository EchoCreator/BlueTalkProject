package com.example.server.mapper;

import com.example.pojo.entity.UserInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserInfoMapper {
    @Select("select * from user_info where user_id=#{id}")
    UserInfo getUserInfoByUserId(Long id);

    @Insert("insert into user_info(user_id, fans, followee, credits, level, create_time, update_time)" +
            " values (#{userId},#{fans},#{followee},#{credits},#{level},now(),now())")
    void addUserInfo(UserInfo userInfo);
}
