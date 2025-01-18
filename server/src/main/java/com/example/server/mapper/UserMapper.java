package com.example.server.mapper;

import com.example.pojo.dto.UserLoginDTO;
import com.example.pojo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("select * from user where phone_number=#{phoneNumber}")
    User findUserByPhoneNumber(String phoneNumber);

    void register(User user);

    @Select("select * from user where id=#{id}")
    User getUserById(Long id);
}
