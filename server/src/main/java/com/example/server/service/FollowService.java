package com.example.server.service;

import com.example.pojo.vo.FollowUserVO;

import java.util.List;

public interface FollowService {
    List<FollowUserVO> getFollowee(Long userId);

    List<FollowUserVO> getFans(Long userId);

    List<FollowUserVO> getMutualFollowing(Long userId);

    Integer isFollowed(Long followUserId);

    void followUser(Long followUserId, Integer isFollowed);
}
