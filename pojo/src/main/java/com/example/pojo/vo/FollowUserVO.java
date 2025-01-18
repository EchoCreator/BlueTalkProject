package com.example.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowUserVO implements Serializable {
    private Long userId;
    private String username;
    private String introduction;
    private String profilePicture;
    private Integer isFollowed;
}
