package com.example.server.controller.user;

import com.example.common.result.Result;
import com.example.pojo.vo.FollowUserVO;
import com.example.server.service.FollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/follow")
public class FollowController {
    @Autowired
    private FollowService followService;

    @GetMapping("/getFollowee")
    public Result<List<FollowUserVO>> getFollowee(Long userId) {
        List<FollowUserVO> list=followService.getFollowee(userId);
        return Result.success(list);
    }

    @GetMapping("/getFans")
    public Result<List<FollowUserVO>> getFans(Long userId) {
        List<FollowUserVO> list=followService.getFans(userId);
        return Result.success(list);
    }

    @GetMapping("/getMutualFollowing")
    public Result<List<FollowUserVO>> getMutualFollowing(Long userId) {
        List<FollowUserVO> list=followService.getMutualFollowing(userId);
        return Result.success(list);
    }

    @GetMapping("/isFollowed")
    public Result<Integer> isFollowed(Long followUserId) {
        Integer b=followService.isFollowed(followUserId);
        return Result.success(b);
    }

    @PostMapping("/followUser")
    public Result followUser(Long followUserId, Integer isFollowed) {
        followService.followUser(followUserId,isFollowed);
        return Result.success();
    }

}
