package com.example.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Follow {
    private Long id;
    private Long userId;
    private Long followerId;
    private LocalDateTime createdTime;
}
