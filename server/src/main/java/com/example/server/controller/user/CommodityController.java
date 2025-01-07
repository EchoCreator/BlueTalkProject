package com.example.server.controller.user;

import com.example.common.result.Result;
import com.example.pojo.vo.CommodityCommentsVO;
import com.example.pojo.vo.CommodityInfoVO;
import com.example.pojo.vo.CommodityTypeVO;
import com.example.pojo.vo.CommodityVO;
import com.example.server.service.CommodityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/commodity")
public class CommodityController {
    @Autowired
    private CommodityService commodityService;

    @GetMapping("/commodityType")
    public Result<List<CommodityTypeVO>> getCommodityType() {
        List<CommodityTypeVO> commodityTypeVOList = commodityService.getCommodityType();
        return Result.success(commodityTypeVOList);
    }

    @GetMapping("/getCommodity")
    public Result<List<CommodityVO>> getCommodity(String name, Integer type) {
        List<CommodityVO> list = commodityService.getCommodity(name, type);
        return Result.success(list);
    }

    @GetMapping("/getCommodityInfo")
    public Result<CommodityInfoVO> getCommodityInfo(Long commodityId) {
        CommodityInfoVO commodityInfoVO = commodityService.getCommodityInfo(commodityId);
        return Result.success(commodityInfoVO);
    }

    @GetMapping("/getCommodityComments")
    public Result<List<CommodityCommentsVO>> getCommodityComments(Long commodityId) {
        List<CommodityCommentsVO> commodityCommentsVOList = commodityService.getCommodityComments(commodityId);
        return Result.success(commodityCommentsVOList);
    }

    @GetMapping("/getUsersCommodity")
    public Result<List<CommodityVO>> getUsersCommodity(Long userId) {
        List<CommodityVO> commodity = commodityService.getUsersCommodity(userId);
        return Result.success(commodity);
    }

}
