package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedisDataVO implements Serializable {
    private Object data;
    private List<Object> dataList;
    private LocalDateTime expireTime;
}
