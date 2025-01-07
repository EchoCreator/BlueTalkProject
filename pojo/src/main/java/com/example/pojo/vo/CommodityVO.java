package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommodityVO implements Serializable {
    private Long id;
    private Long userId;
    private String name;
    private Long typeId;
    private String images;
    private BigDecimal price;
    private Integer sold;
    private String address;
    private Integer deliveryTime;
}
