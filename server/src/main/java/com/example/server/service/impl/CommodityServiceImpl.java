package com.example.server.service.impl;

import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.entity.*;
import com.example.pojo.vo.*;
import com.example.server.mapper.CommodityMapper;
import com.example.server.mapper.UserInfoMapper;
import com.example.server.mapper.UserMapper;
import com.example.server.service.CommodityService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CommodityServiceImpl implements CommodityService {
    @Autowired
    private CommodityMapper commodityMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<CommodityTypeVO> getCommodityType() {
        String key = SystemConstant.REDIS_COMMODITY_TYPE_KEY;
        List<CommodityTypeVO> commodityTypeVOList = (List<CommodityTypeVO>) redisTemplate.opsForValue().get(key);

        // 在redis中查询
        if (commodityTypeVOList != null) {
            return commodityTypeVOList;
        }

        // 如果不存在，则在数据库中查询
        List<CommodityType> list = commodityMapper.getCommodityType();
        commodityTypeVOList = new ArrayList<>();
        for (CommodityType commodityType : list) {
            CommodityTypeVO commodityTypeVO = CommodityTypeVO.builder()
                    .id(commodityType.getId())
                    .name(commodityType.getName())
                    .sort(commodityType.getSort())
                    .build();
            commodityTypeVOList.add(commodityTypeVO);
        }

        // 保存在redis中
        redisTemplate.opsForValue().set(key, commodityTypeVOList, SystemConstant.REDIS_COMMODITY_TYPE_EXPIRATION, TimeUnit.DAYS);

        return commodityTypeVOList;
    }

    @Override
    public List<CommodityVO> getCommodity(String name, Integer type) {
        String key = SystemConstant.REDIS_COMMODITY_LIST_KEY;
        List<CommodityVO> commodityVOList = (List<CommodityVO>) redisTemplate.opsForValue().get(key + type);
        List<CommodityVO> newList = new ArrayList<>();
        if (commodityVOList != null) {
            if (name != null && !name.isEmpty()) {
                newList = commodityVOList.stream()
                        .filter(commodity -> commodity.getName().contains(name))
                        .toList();
            } else {
                newList = commodityVOList;
            }
            return newList;
        }

        List<Commodity> list = commodityMapper.getCommodityByType(type);
        commodityVOList = new ArrayList<>();
        for (Commodity commodity : list) {
            CommodityVO commodityVO = CommodityVO.builder()
                    .id(commodity.getId())
                    .userId(commodity.getUserId())
                    .name(commodity.getName())
                    .typeId(commodity.getTypeId())
                    .images(commodity.getImages())
                    .price(commodity.getPrice())
                    .sold(commodity.getSold())
                    .address(commodity.getAddress())
                    .deliveryTime(commodity.getDeliveryTime())
                    .build();
            if (commodityVO.getName().contains(name)) {
                newList.add(commodityVO);
            }
            commodityVOList.add(commodityVO);
        }
        redisTemplate.opsForValue().set(key, commodityVOList, SystemConstant.REDIS_COMMODITY_LIST_EXPIRATION, TimeUnit.MINUTES);

        return newList;
    }

    @Override
    public CommodityInfoVO getCommodityInfo(Long commodityId) {
        String commodity_key = SystemConstant.REDIS_COMMODITY_KEY + commodityId;
        String commodity_comments_key = SystemConstant.REDIS_COMMODITY_COMMENTS_KEY + commodityId;

        // 查找商品信息
        CommodityVO commodityVO = (CommodityVO) redisTemplate.opsForValue().get(commodity_key);
        if (commodityVO == null) {
            Commodity commodity = commodityMapper.getCommodityById(commodityId);
            commodityVO = CommodityVO.builder()
                    .id(commodity.getId())
                    .userId(commodity.getUserId())
                    .name(commodity.getName())
                    .typeId(commodity.getTypeId())
                    .images(commodity.getImages())
                    .price(commodity.getPrice())
                    .sold(commodity.getSold())
                    .address(commodity.getAddress())
                    .deliveryTime(commodity.getDeliveryTime())
                    .build();
            redisTemplate.opsForValue().set(commodity_key, commodityVO, SystemConstant.REDIS_COMMODITY_EXPIRATION, TimeUnit.MINUTES);
        }

        // 查找商品评论
        List<CommodityCommentsVO> commodityCommentsVOList = (List<CommodityCommentsVO>) redisTemplate.opsForValue().get(commodity_comments_key);
        if (commodityCommentsVOList == null) {
            List<CommodityComments> list = commodityMapper.getCommodityComments(commodityId);
            commodityCommentsVOList = new ArrayList<>();
            for (CommodityComments c : list) {
                LocalDate creatTime = c.getCreateTime().toLocalDate();
                LocalDate updateTime = c.getUpdateTime().toLocalDate();
                User user = userMapper.getUserById(c.getUserId());
                CommodityCommentsVO commodityCommentsVO = CommodityCommentsVO.builder()
                        .id(c.getId())
                        .userId(c.getUserId())
                        .commodityId(c.getCommodityId())
                        .username(user.getUsername())
                        .profilePicture(user.getProfilePicture())
                        .content(c.getContent())
                        .score(c.getScore())
                        .createTime(creatTime)
                        .updateTime(updateTime)
                        .build();
                commodityCommentsVOList.add(commodityCommentsVO);
            }
            redisTemplate.opsForValue().set(commodity_comments_key, commodityCommentsVOList, SystemConstant.REDIS_COMMODITY_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
        }

        // 查找商铺主的用户信息
        String user_key = SystemConstant.REDIS_USER_KEY + commodityVO.getUserId();
        UserVO userVO = (UserVO) redisTemplate.opsForValue().get(user_key);
        if (userVO == null) {
            userVO = getUserInfo(user_key, commodityVO.getUserId());
        }


        CommodityInfoVO commodityInfoVO = CommodityInfoVO.builder()
                .commodity(commodityVO)
                .commodityCommentsList(commodityCommentsVOList)
                .user(userVO)
                .build();

        return commodityInfoVO;
    }

    @Override
    public List<CommodityCommentsVO> getCommodityComments(Long commodityId) {
        String commodity_comments_key = SystemConstant.REDIS_COMMODITY_COMMENTS_KEY + commodityId;
        List<CommodityCommentsVO> commodityCommentsVOList = (List<CommodityCommentsVO>) redisTemplate.opsForValue().get(commodity_comments_key);

        if (commodityCommentsVOList != null) {
            return commodityCommentsVOList;
        }

        List<CommodityComments> list = commodityMapper.getCommodityComments(commodityId);
        commodityCommentsVOList = new ArrayList<>();
        for (CommodityComments c : list) {
            LocalDate creatTime = c.getCreateTime().toLocalDate();
            LocalDate updateTime = c.getUpdateTime().toLocalDate();
            User user = userMapper.getUserById(c.getUserId());
            CommodityCommentsVO commodityCommentsVO = CommodityCommentsVO.builder()
                    .id(c.getId())
                    .userId(c.getUserId())
                    .commodityId(c.getCommodityId())
                    .username(user.getUsername())
                    .profilePicture(user.getProfilePicture())
                    .content(c.getContent())
                    .score(c.getScore())
                    .createTime(creatTime)
                    .updateTime(updateTime)
                    .build();
            commodityCommentsVOList.add(commodityCommentsVO);
        }
        redisTemplate.opsForValue().set(commodity_comments_key, commodityCommentsVOList, SystemConstant.REDIS_COMMODITY_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
        return commodityCommentsVOList;
    }

    @Override
    public List<CommodityVO> getUsersCommodity(Long userId) {
        List<Commodity> commodityList=commodityMapper.getCommodityByUserId(userId);
        List<CommodityVO> commodityVOList=new ArrayList<>();
        for (Commodity commodity : commodityList) {
            CommodityVO commodityVO = CommodityVO.builder()
                    .id(commodity.getId())
                    .userId(commodity.getUserId())
                    .name(commodity.getName())
                    .typeId(commodity.getTypeId())
                    .images(commodity.getImages())
                    .price(commodity.getPrice())
                    .sold(commodity.getSold())
                    .address(commodity.getAddress())
                    .deliveryTime(commodity.getDeliveryTime())
                    .build();
            commodityVOList.add(commodityVO);
        }
        return commodityVOList;
    }

    public UserVO getUserInfo(String key, Long userId) {
        UserVO userVO = (UserVO) redisTemplate.opsForValue().get(key);

        if (userVO != null) {
            return userVO;
        }

        User user = userMapper.getUserById(userId);
        UserInfo userInfo = userInfoMapper.getUserInfoByUserId(userId);

        // 计算年龄
        LocalDate now = LocalDate.now();
        Integer age = null;
        if (userInfo.getBirthday() != null) {
            age = userInfo.getBirthday().until(now).getYears();
        }

        userVO = UserVO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .profilePicture(user.getProfilePicture())
                .city(userInfo.getCity())
                .introduction(userInfo.getIntroduction())
                .fans(userInfo.getFans())
                .followee(userInfo.getFollowee())
                .gender(userInfo.getGender())
                .age(age)
                .school(userInfo.getSchool())
                .credits(userInfo.getCredits())
                .level(userInfo.getLevel())
                .build();

        redisTemplate.opsForValue().set(key, userVO, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES);

        return userVO;
    }
}
