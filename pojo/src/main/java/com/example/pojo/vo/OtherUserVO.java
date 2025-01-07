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
public class OtherUserVO implements Serializable {
    private Long id;
    private String username;
    private String profilePicture;
    private String city;
    private String introduction;
    private Integer fans;
    private Integer followee;
    private Integer gender;
    private Integer age;
    private String school;
    private Integer level;
}
