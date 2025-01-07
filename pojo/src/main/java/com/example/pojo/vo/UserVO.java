package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserVO implements Serializable {
    private Long id;
    private String phoneNumber;
    private String username;
    private String profilePicture;
    private String city;
    private String introduction;
    private Integer fans;
    private Integer followee;
    private Integer gender;
    private Integer age;
    private String school;
    private Integer credits;
    private Integer level;
}
