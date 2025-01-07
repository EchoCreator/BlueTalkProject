package com.example.server.service;

import com.example.pojo.vo.CommodityCommentsVO;
import com.example.pojo.vo.CommodityInfoVO;
import com.example.pojo.vo.CommodityTypeVO;
import com.example.pojo.vo.CommodityVO;

import java.util.List;

public interface CommodityService {
    List<CommodityTypeVO> getCommodityType();

    List<CommodityVO> getCommodity(String name, Integer type);

    CommodityInfoVO getCommodityInfo(Long commodityId);

    List<CommodityCommentsVO> getCommodityComments(Long commodityId);

    List<CommodityVO> getUsersCommodity(Long userId);
}
