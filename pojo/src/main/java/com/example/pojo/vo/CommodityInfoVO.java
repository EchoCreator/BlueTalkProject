package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommodityInfoVO implements Serializable {
    private CommodityVO commodity;
    private List<CommodityCommentsVO> commodityCommentsList;
    private UserVO user;
}
