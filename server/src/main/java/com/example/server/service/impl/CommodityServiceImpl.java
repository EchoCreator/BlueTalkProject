package com.example.server.service.impl;

import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.exception.NoDataInDBException;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.entity.*;
import com.example.pojo.vo.*;
import com.example.server.mapper.CommodityMapper;
import com.example.server.mapper.UserInfoMapper;
import com.example.server.mapper.UserMapper;
import com.example.server.service.CommodityService;
import com.example.server.service.UserService;
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
    private UserServiceImpl userServiceImpl;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public List<CommodityTypeVO> getCommodityType() {
        String key = SystemConstant.REDIS_COMMODITY_TYPE_KEY;
        List<CommodityType> list = redisUtil.queryListWithCachePenetration(key, null, CommodityType.class, null, this::getCommodityTypeFromDB, SystemConstant.REDIS_COMMODITY_TYPE_EXPIRATION, TimeUnit.DAYS).getData();
        if (list == null) {
            throw new NoDataInDBException("商品类别数据为空！");
        }

        List<CommodityTypeVO> commodityTypeVOList = new ArrayList<>();
        for (CommodityType commodityType : list) {
            CommodityTypeVO commodityTypeVO = CommodityTypeVO.builder()
                    .id(commodityType.getId())
                    .name(commodityType.getName())
                    .sort(commodityType.getSort())
                    .build();
            commodityTypeVOList.add(commodityTypeVO);
        }

        return commodityTypeVOList;
    }

    @Override
    public List<CommodityVO> getCommodity(String name, Long typeId) {
        String key = SystemConstant.REDIS_COMMODITY_LIST_KEY;
        List<Commodity> commodityList = redisUtil.queryListWithCachePenetration(key, typeId, Commodity.class, this::getCommodityByTypeFromDB, null, SystemConstant.REDIS_COMMODITY_LIST_EXPIRATION, TimeUnit.MINUTES).getData();
        if (commodityList == null) {
            throw new NoDataInDBException("该类别还没有商品！");
        }
        List<CommodityVO> commodityVOList = new ArrayList<>();
        for (Commodity commodity : commodityList) {
            if (commodity.getName().contains(name)) {
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
        }
        return commodityVOList;
    }

    @Override
    public CommodityInfoVO getCommodityInfo(Long commodityId) {
        String commodity_key = SystemConstant.REDIS_COMMODITY_KEY;

        // 查找商品信息
        Commodity commodity = redisUtil.queryWithCachePenetration(commodity_key, commodityId, Commodity.class, this::getCommodityByIdFromDB, null, SystemConstant.REDIS_COMMODITY_EXPIRATION, TimeUnit.MINUTES).getData();
        if (commodity == null) {
            throw new NoDataInDBException("暂无该商品信息！");
        }
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

        // 查找商品评论
        List<CommodityComments> list = getCommodityCommentsFromCache(commodityId);
        List<CommodityCommentsVO> commodityCommentsVOList = new ArrayList<>();
        for (CommodityComments c : list) {
            LocalDate creatTime = c.getCreateTime().toLocalDate();
            LocalDate updateTime = c.getUpdateTime().toLocalDate();
            User user = userServiceImpl.getUserFromCache(c.getUserId());
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

        // 查找商铺主的用户信息
        OtherUserVO shopkeeper = userServiceImpl.getOtherUserInfo(commodity.getUserId());

        CommodityInfoVO commodityInfoVO = CommodityInfoVO.builder()
                .commodity(commodityVO)
                .commodityCommentsList(commodityCommentsVOList)
                .user(shopkeeper)
                .build();

        return commodityInfoVO;
    }

    @Override
    public List<CommodityCommentsVO> getCommodityComments(Long commodityId) {
        List<CommodityComments> list = getCommodityCommentsFromCache(commodityId);
        List<CommodityCommentsVO> commodityCommentsVOList = new ArrayList<>();
        for (CommodityComments c : list) {
            LocalDate creatTime = c.getCreateTime().toLocalDate();
            LocalDate updateTime = c.getUpdateTime().toLocalDate();
            User user = userServiceImpl.getUserFromCache(c.getUserId());
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
        return commodityCommentsVOList;
    }

    @Override
    public List<CommodityVO> getUsersCommodity(Long userId) {
        String key=SystemConstant.REDIS_USER_COMMODITY_KEY;
        List<Commodity> commodityList =redisUtil.queryListWithCachePenetration(key,userId,Commodity.class,this::getCommodityByUserIdFromDB, null, SystemConstant.REDIS_USER_COMMODITY_EXPIRATION, TimeUnit.MINUTES).getData();
        if (commodityList == null) {
            throw new NoDataInDBException("该用户还没有任何店铺商品！");
        }
        List<CommodityVO> commodityVOList = new ArrayList<>();
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

    public List<CommodityType> getCommodityTypeFromDB() {
        return commodityMapper.getCommodityType();
    }

    public List<Commodity> getCommodityByTypeFromDB(Long typeId) {
        return commodityMapper.getCommodityByType(typeId);
    }

    public Commodity getCommodityByIdFromDB(Long commodityId) {
        return commodityMapper.getCommodityById(commodityId);
    }

    public List<CommodityComments> getCommodityCommentsByIdFromDB(Long commodityId) {
        return commodityMapper.getCommodityComments(commodityId);
    }

    public List<Commodity> getCommodityByUserIdFromDB(Long userId) {
        return commodityMapper.getCommodityByUserId(userId);
    }

    public List<CommodityComments> getCommodityCommentsFromCache(Long commodityId) {
        List<CommodityComments> commodityComments = redisUtil.queryListWithCachePenetration(SystemConstant.REDIS_COMMODITY_COMMENTS_KEY, commodityId, CommodityComments.class, this::getCommodityCommentsByIdFromDB, null, SystemConstant.REDIS_COMMODITY_COMMENTS_EXPIRATION, TimeUnit.MINUTES).getData();
        if (commodityComments == null) {
            throw new NoDataInDBException("该商品还没有评论！");
        }
        return commodityComments;
    }
}
