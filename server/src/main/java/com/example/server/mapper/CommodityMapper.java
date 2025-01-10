package com.example.server.mapper;

import com.example.pojo.entity.Commodity;
import com.example.pojo.entity.CommodityComments;
import com.example.pojo.entity.CommodityType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommodityMapper {
    @Select("select * from commodity_type order by sort")
    List<CommodityType> getCommodityType();

    List<Commodity> getCommodityByType(Long typeId);

    @Select("select * from commodity where id=#{commodityId}")
    Commodity getCommodityById(Long commodityId);

    @Select("select * from commodity_comments where commodity_id=#{commodityId}")
    List<CommodityComments> getCommodityComments(Long commodityId);

    @Select("select * from commodity where user_id=#{userId}")
    List<Commodity> getCommodityByUserId(Long userId);
}
