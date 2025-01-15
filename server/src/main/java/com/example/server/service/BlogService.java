package com.example.server.service;

import com.example.pojo.dto.BlogCommentsDTO;
import com.example.pojo.vo.BlogContentVO;
import com.example.pojo.vo.BlogVO;

import java.util.List;

public interface BlogService {
    List<BlogVO> getBlogs();

    BlogContentVO getBlogContent(Long blogId);

    List<BlogVO> getUsersBlogs(Long userId);

    List<BlogVO> getUsersLikedFavoriteBlogs(Long userId);

    void likeBlog(Long blogId);

    void favoriteBlog(Long blogId);

    void postBlogComment(BlogCommentsDTO blogCommentsDTO);
}
