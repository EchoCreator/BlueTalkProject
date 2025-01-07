package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CommodityCommentsVO implements Serializable {
    private Long id;
    private Long userId;
    private Long commodityId;
    private String username;
    private String profilePicture;
    private String content;
    private Integer score;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate createTime;
    @JsonFormat(pattern = "yyyy/MM/dd")
    private LocalDate updateTime;
}
