package com.example.server.controller.user;

import com.example.common.result.Result;
import com.example.common.utils.RedisUtil;
import com.example.pojo.dto.BlogCommentsDTO;
import com.example.pojo.dto.BlogDTO;
import com.example.pojo.vo.BlogContentVO;
import com.example.pojo.vo.BlogVO;
import com.example.server.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/blog")
public class BlogController {
    @Autowired
    private BlogService blogService;

    @GetMapping("/getBlogs")
    public Result<List<BlogVO>> getBlogs() {
        List<BlogVO> list = blogService.getBlogs();
        return Result.success(list);
    }

    @GetMapping("/getBlogContent")
    public Result<BlogContentVO> getBlogContent(Long blogId) {
        BlogContentVO blogContentVO = blogService.getBlogContent(blogId);
        return Result.success(blogContentVO);
    }

    @GetMapping("/getUsersBlogs")
    public Result<List<BlogVO>> getUsersBlogs(Long userId) {
        List<BlogVO> list = blogService.getUsersBlogs(userId);
        return Result.success(list);
    }

    @GetMapping("/getUsersLikedFavoriteBlogs")
    public Result<List<BlogVO>> getMyLikedFavoriteBlogs(Long userId) {
        List<BlogVO> list = blogService.getUsersLikedFavoriteBlogs(userId);
        return Result.success(list);
    }

    @PostMapping("/likeBlog")
    public Result likeBlog(Long blogId) {
        blogService.likeBlog(blogId);
        return Result.success();
    }

    @PostMapping("/favoriteBlog")
    public Result favoriteBlog(Long blogId) {
        blogService.favoriteBlog(blogId);
        return Result.success();
    }

    @PostMapping("/postBlogComment")
    public Result postBlogComment(@RequestBody BlogCommentsDTO blogCommentsDTO) {
        blogService.postBlogComment(blogCommentsDTO);
        return Result.success();
    }

    @PostMapping("/postBlog")
    public Result postBlog(@RequestBody BlogDTO blogDTO) {
        blogService.postBlog(blogDTO);
        return Result.success();
    }
}
