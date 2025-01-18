package com.example.server.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.exception.NoDataInDBException;
import com.example.common.result.QueryRedisResult;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.entity.User;
import com.example.pojo.entity.UserInfo;
import com.example.pojo.vo.FollowUserVO;
import com.example.pojo.vo.OtherUserVO;
import com.example.server.mapper.FollowMapper;
import com.example.server.mapper.UserInfoMapper;
import com.example.server.mapper.UserMapper;
import com.example.server.service.FollowService;
import com.google.gson.JsonArray;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Service
public class FollowServiceImpl implements FollowService {
    @Autowired
    private FollowMapper followMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisUtil redisUtil;

    public void getMyFolloweeFans(Long myUserId) {
        // 获取关注用户
        String followeeKey = SystemConstant.REDIS_FOLLOWEE_KEY + myUserId;
        Set<String> followeeSet = stringRedisTemplate.opsForSet().members(followeeKey);

        if (followeeSet == null || followeeSet.isEmpty()) {
            List<Long> followee = findFolloweeFromDB(myUserId);
            for (Long id : followee) {
                stringRedisTemplate.opsForSet().add(followeeKey, id.toString());
            }
        }

        // 获取粉丝
        String fansKey = SystemConstant.REDIS_FANS_KEY + myUserId;
        Set<String> fansSet = stringRedisTemplate.opsForSet().members(fansKey);
        if (fansSet == null || fansSet.isEmpty()) {
            List<Long> fans = findFansFromDB(myUserId);
            for (Long id : fans) {
                stringRedisTemplate.opsForSet().add(fansKey, id.toString());
            }
        }
    }

    @Override
    public List<FollowUserVO> getFollowee(Long userId) {
        String followeeKey = SystemConstant.REDIS_FOLLOWEE_KEY + userId;
        Set<String> s = stringRedisTemplate.opsForSet().members(followeeKey);

        List<FollowUserVO> list = findUserFolloweeFans(followeeKey, s, userId, this::findFolloweeFromDB);
        return list;
    }

    @Override
    public List<FollowUserVO> getFans(Long userId) {
        String fansKey = SystemConstant.REDIS_FANS_KEY + userId;
        Set<String> s = stringRedisTemplate.opsForSet().members(fansKey);

        List<FollowUserVO> list = findUserFolloweeFans(fansKey, s, userId, this::findFansFromDB);
        return list;
    }

    @Override
    public List<FollowUserVO> getMutualFollowing(Long userId) {
        List<FollowUserVO> followee = getFollowee(userId);
        List<FollowUserVO> fans = getFans(userId);
        followee.retainAll(fans);
        return followee;
    }

    @Override
    public Integer isFollowed(Long followUserId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());
        /*Integer isFollowed = followMapper.isFollowed(userId, followUserId);*/
        Set<String> myFollowee = stringRedisTemplate.opsForSet().members(SystemConstant.REDIS_FOLLOWEE_KEY + userId);
        Integer isFollowed = 0;

        if (myFollowee.contains(followUserId.toString())) {
            isFollowed = 1;
        }

        return isFollowed;
    }

    @Override
    public void followUser(Long followUserId, Integer isFollowed) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String followeeKey = SystemConstant.REDIS_FOLLOWEE_KEY + userId;

        // 如果未关注，则执行关注操作；否则执行取关操作
        Integer ops = 1;
        if (isFollowed == 0) {
            followMapper.followUser(userId, followUserId);
            stringRedisTemplate.opsForSet().add(followeeKey, followUserId.toString());
        } else {
            followMapper.cancelFollowUser(userId, followUserId);
            stringRedisTemplate.opsForSet().remove(followeeKey, followUserId.toString());
            ops = -1;
        }

        userInfoMapper.updateUsersFollowee(userId, ops);
        userInfoMapper.updateUsersFans(followUserId, ops);

        // 更新redis
        QueryRedisResult<UserInfo> q1=redisUtil.queryWithCachePenetration(SystemConstant.REDIS_USER_INFO_KEY, userId, UserInfo.class, userServiceImpl::getUserInfoByIdFromDB, null, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES);
        if(!q1.getFlag()){
            UserInfo userInfo = q1.getData();
            userInfo.setFollowee(userInfo.getFollowee() + ops);
            redisUtil.set(SystemConstant.REDIS_USER_INFO_KEY + userId, userInfo, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES);
        }

        QueryRedisResult<UserInfo> q2=redisUtil.queryWithCachePenetration(SystemConstant.REDIS_USER_INFO_KEY, followUserId, UserInfo.class, userServiceImpl::getUserInfoByIdFromDB, null, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES);
        if(!q2.getFlag()){
            UserInfo userInfo = q2.getData();
            userInfo.setFans(userInfo.getFans() + ops);
            redisUtil.set(SystemConstant.REDIS_USER_INFO_KEY + followUserId, userInfo, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES);
        }
    }

    public List<FollowUserVO> findUserFolloweeFans(String key, Set<String> s, Long userId, Function<Long, List<Long>> getDataFromDB) {
        List<Long> userIds = new ArrayList<>();
        List<FollowUserVO> followUserVOS = new ArrayList<>();

        Claims claims = ThreadLocalUtil.get();
        Long myUserId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());
        Set<String> myFollowee = stringRedisTemplate.opsForSet().members(SystemConstant.REDIS_FOLLOWEE_KEY + myUserId);

        if (s != null && !s.isEmpty()) {
            for (String str : s) {
                userIds.add(Long.valueOf(str));
            }
        } else {
            userIds = getDataFromDB.apply(userId);
            if (userIds == null) {
                throw new NoDataInDBException("该用户还没有任何关注或粉丝");
            }
            for (Long id : userIds) {
                stringRedisTemplate.opsForSet().add(key, id.toString());
            }
        }

        for (Long id : userIds) {
            assert myFollowee != null;
            int isFollowed = 0;
            if (myFollowee.contains(id.toString())) {
                isFollowed = 1;
            }

            OtherUserVO u = userServiceImpl.getOtherUserInfo(id);
            FollowUserVO followUserVO = FollowUserVO.builder()
                    .userId(id)
                    .username(u.getUsername())
                    .introduction(u.getIntroduction())
                    .profilePicture(u.getProfilePicture())
                    .isFollowed(isFollowed)
                    .build();
            followUserVOS.add(followUserVO);
        }
        return followUserVOS;
    }

    public List<Long> findFolloweeFromDB(Long userId) {
        return followMapper.getFollowee(userId);
    }

    public List<Long> findFansFromDB(Long userId) {
        return followMapper.getFans(userId);
    }
}
