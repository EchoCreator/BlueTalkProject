package com.example.server.service;

import com.example.common.result.Result;
import com.example.pojo.dto.UserLoginDTO;
import com.example.pojo.vo.OtherUserVO;
import com.example.pojo.vo.UserLoginVO;
import com.example.pojo.vo.UserVO;

public interface UserService {
    void getCode(String phoneNumber);

    UserLoginVO login(UserLoginDTO userLoginDTO);

    UserVO getUserInfo();

    OtherUserVO getOtherUserInfo(Long userId);
}
