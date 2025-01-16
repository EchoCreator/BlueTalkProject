package com.example.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogDTO {
    private String title;
    private String images;
    private String content;
    private String tags;
    private String address;
}
