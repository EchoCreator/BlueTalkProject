package com.example.server.controller.user;

import com.example.common.result.Result;
import com.example.pojo.dto.UserLoginDTO;
import com.example.pojo.vo.OtherUserVO;
import com.example.pojo.vo.UserLoginVO;
import com.example.pojo.vo.UserVO;
import com.example.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/getCode")
    public Result getCode(String phoneNumber) {
        userService.getCode(phoneNumber);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO) {
        UserLoginVO userLoginVO = userService.login(userLoginDTO);
        return Result.success(userLoginVO);
    }

    @GetMapping("/userInfo")
    public Result<UserVO> getUserInfo() {
        UserVO userVO=userService.getUserInfo();
        return Result.success(userVO);
    }

    @GetMapping("/otherUserInfo")
    public Result<OtherUserVO> getOtherUserInfo(Long userId) {
        OtherUserVO otherUserVO=userService.getOtherUserInfo(userId);
        return Result.success(otherUserVO);
    }
}
