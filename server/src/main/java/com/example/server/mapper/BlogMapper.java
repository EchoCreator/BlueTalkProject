package com.example.server.mapper;

import com.example.pojo.entity.Blog;
import com.example.pojo.entity.BlogComments;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BlogMapper {
    @Select("select * from blog order by create_time desc")
    List<Blog> getBlogs();

    @Select("select * from blog where id=#{id}")
    Blog getBlogById(Long id);

    @Select("select * from blog_comments where blog_id=#{blogId} and parent_id=0")
    List<BlogComments> getBlogParentComments(Long blogId);

    @Select("select * from blog_comments where blog_id=#{blogId} and parent_id=#{parentId}")
    List<BlogComments> getBlogChildrenComments(Long blogId, Long parentId);

    @Select("SELECT COUNT(*) FROM blog_likes WHERE user_id = #{userId} and blog_id=#{blogId}")
    Integer isLiked(Long userId, Long blogId);

    @Select("SELECT COUNT(*) FROM blog_favorites WHERE user_id = #{userId} and blog_id=#{blogId}")
    Integer isFavorite(Long userId, Long blogId);

    @Insert("insert into blog_likes(blog_id, user_id, create_time, update_time)" +
            "values (#{blogId}, #{userId},now(),now())")
    void DoLikeBlog(Long userId, Long blogId);

    @Delete("delete from blog_likes where user_id=#{userId} and blog_id=#{blogId}")
    void CancelLikeBlog(Long userId, Long blogId);

    @Insert("insert into blog_favorites(blog_id, user_id, create_time, update_time)" +
            "values (#{blogId}, #{userId},now(),now())")
    void DoFavoriteBlog(Long userId, Long blogId);

    @Delete("delete from blog_favorites where user_id=#{userId} and blog_id=#{blogId}")
    void CancelFavoriteBlog(Long userId, Long blogId);

    @Update("update blog set likes = likes+1 where id=#{blogId};")
    void increaseLikeNum(Long blogId);

    @Update("update blog set likes = likes-1 where id=#{blogId};")
    void decreaseLikeNum(Long blogId);

    @Update("update blog set favorites = blog.favorites+1 where id=#{blogId};")
    void increaseFavoriteNum(Long blogId);

    @Update("update blog set favorites = blog.favorites-1 where id=#{blogId};")
    void decreaseFavoriteNum(Long blogId);

    void postBlogComment(BlogComments blogComments);

    @Update("update blog set comments=comments+1 where id=#{blogId}")
    void updateBlogCommentNum(Long blogId);

    void postBlog(Blog blog);
}
