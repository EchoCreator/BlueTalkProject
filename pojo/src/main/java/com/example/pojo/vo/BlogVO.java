package com.example.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlogVO implements Serializable {
    private Long id;
    private Long userId;
    private String username;
    private String profilePicture;
    private String title;
    private String content;
    private String images;
    private String tags;
    private Integer likes;
    private Integer favorites;
    private Integer isLiked;
    private Integer IsFavorite;
    private Integer otherUserLiked;
    private Integer otherUserFavorite;
    private Integer comments;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime createdTime;
    @JsonFormat(pattern = "yyyy/MM/dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
