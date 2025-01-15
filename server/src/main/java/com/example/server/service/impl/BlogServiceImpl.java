package com.example.server.service.impl;

import com.example.common.constant.BlogCommentsConstant;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.dto.BlogCommentsDTO;
import com.example.pojo.entity.Blog;
import com.example.pojo.entity.BlogComments;
import com.example.pojo.vo.*;
import com.example.server.mapper.BlogMapper;
import com.example.server.service.BlogService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class BlogServiceImpl implements BlogService {
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserServiceImpl userServiceImpl;

    @Override
    public List<BlogVO> getBlogs() {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        List<BlogVO> blogVOList = new ArrayList<>();
        for (Blog b : blogList) {
            OtherUserVO otherUserVO = userServiceImpl.getOtherUserInfo(b.getUserId());
            Integer isLiked = blogMapper.isLiked(userId, b.getId());
            Integer isFavorite = blogMapper.isFavorite(userId, b.getId());

            BlogVO blogVO = BlogVO.builder()
                    .id(b.getId())
                    .userId(b.getUserId())
                    .username(otherUserVO.getUsername())
                    .profilePicture(otherUserVO.getProfilePicture())
                    .title(b.getTitle())
                    .content(b.getContent())
                    .images(b.getImages())
                    .tags(b.getTags())
                    .likes(b.getLikes())
                    .favorites(b.getFavorites())
                    .isLiked(isLiked)
                    .IsFavorite(isFavorite)
                    .comments(b.getComments())
                    .createdTime(b.getCreatedTime())
                    .updatedTime(b.getUpdatedTime())
                    .build();
            blogVOList.add(blogVO);
        }
        return blogVOList;
    }

    @Override
    public BlogContentVO getBlogContent(Long blogId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        BlogVO blogVO = new BlogVO();
        for (Blog b : blogList) {
            if (blogId.equals(b.getId())) {
                OtherUserVO otherUserVO = userServiceImpl.getOtherUserInfo(b.getUserId());

                Integer isLiked = blogMapper.isLiked(userId, b.getId());
                Integer isFavorite = blogMapper.isFavorite(userId, b.getId());

                blogVO = BlogVO.builder()
                        .id(b.getId())
                        .userId(b.getUserId())
                        .username(otherUserVO.getUsername())
                        .profilePicture(otherUserVO.getProfilePicture())
                        .title(b.getTitle())
                        .content(b.getContent())
                        .images(b.getImages())
                        .tags(b.getTags())
                        .likes(b.getLikes())
                        .favorites(b.getFavorites())
                        .isLiked(isLiked)
                        .IsFavorite(isFavorite)
                        .comments(b.getComments())
                        .createdTime(b.getCreatedTime())
                        .updatedTime(b.getUpdatedTime())
                        .build();
                break;
            }
        }

        String blogCommentsKey = SystemConstant.REDIS_BLOG_COMMENTS_KEY;
        String blogChildrenCommentsKey = SystemConstant.REDIS_BLOG_CHILDREN_COMMENTS_KEY;
        List<BlogComments> blogCommentsList = redisUtil.queryListWithCachePenetration(blogCommentsKey, blogId, BlogComments.class, this::getParentBlogCommentsFromDB, null, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
        List<BlogCommentsVO> blogCommentsVOList = new ArrayList<>();

        for (BlogComments b : blogCommentsList) {
            OtherUserVO otherUserVO = userServiceImpl.getOtherUserInfo(b.getUserId());

            // 获取二级评论
            List<BlogComments> blogChildrenCommentsList = redisUtil.queryListWithCachePenetration(blogChildrenCommentsKey, blogId, b.getId(), BlogComments.class, this::getChildrenBlogCommentsFromDB, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            List<BlogCommentsVO> blogChildrenCommentsVOList = new ArrayList<>();

            if (blogChildrenCommentsList != null && !blogChildrenCommentsList.isEmpty()) {
                for (BlogComments cb : blogChildrenCommentsList) {
                    OtherUserVO postUserVO = userServiceImpl.getOtherUserInfo(cb.getUserId());
                    OtherUserVO replyUserVO = new OtherUserVO();
                    if (cb.getReplyId() != 0) {
                        replyUserVO = userServiceImpl.getOtherUserInfo(cb.getReplyUserId());
                    }
                    BlogCommentsVO blogCommentsVO = BlogCommentsVO.builder()
                            .id(cb.getId())
                            .blogId(cb.getBlogId())
                            .userId(cb.getUserId())
                            .parentId(cb.getParentId())
                            .replyId(cb.getReplyId())
                            .replyUserId(cb.getReplyUserId())
                            .replyUsername(replyUserVO.getUsername())
                            .username(postUserVO.getUsername())
                            .profilePicture(postUserVO.getProfilePicture())
                            .content(cb.getContent())
                            .likes(cb.getLikes())
                            .status(cb.getStatus())
                            .createTime(cb.getCreateTime())
                            .updateTime(cb.getUpdateTime())
                            .build();
                    blogChildrenCommentsVOList.add(blogCommentsVO);
                }
            }

            BlogCommentsVO blogCommentsVO = BlogCommentsVO.builder()
                    .id(b.getId())
                    .blogId(b.getBlogId())
                    .userId(b.getUserId())
                    .parentId(b.getParentId())
                    .replyId(b.getReplyId())
                    .username(otherUserVO.getUsername())
                    .profilePicture(otherUserVO.getProfilePicture())
                    .content(b.getContent())
                    .likes(b.getLikes())
                    .status(b.getStatus())
                    .childrenComments(blogChildrenCommentsVOList)
                    .createTime(b.getCreateTime())
                    .updateTime(b.getUpdateTime())
                    .build();
            blogCommentsVOList.add(blogCommentsVO);
        }

        BlogContentVO blogContentVO = BlogContentVO.builder()
                .blog(blogVO)
                .comments(blogCommentsVOList).build();
        return blogContentVO;
    }

    @Override
    public List<BlogVO> getUsersBlogs(Long userId) {
        Claims claims = ThreadLocalUtil.get();
        Long myUserId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        List<BlogVO> blogVOList = new ArrayList<>();

        OtherUserVO user = userServiceImpl.getOtherUserInfo(userId);
        for (Blog b : blogList) {
            if (b.getUserId().equals(userId)) {
                Integer isLiked = blogMapper.isLiked(myUserId, b.getId());
                Integer isFavorite = blogMapper.isFavorite(myUserId, b.getId());

                BlogVO blogVO = BlogVO.builder()
                        .id(b.getId())
                        .userId(b.getUserId())
                        .username(user.getUsername())
                        .profilePicture(user.getProfilePicture())
                        .title(b.getTitle())
                        .content(b.getContent())
                        .images(b.getImages())
                        .tags(b.getTags())
                        .likes(b.getLikes())
                        .favorites(b.getFavorites())
                        .isLiked(isLiked)
                        .IsFavorite(isFavorite)
                        .comments(b.getComments())
                        .createdTime(b.getCreatedTime())
                        .updatedTime(b.getUpdatedTime())
                        .build();
                blogVOList.add(blogVO);
            }
        }
        return blogVOList;
    }

    @Override
    // 获得用户点赞或收藏的帖子（注意：用户点赞或收藏的帖子，还需要查看“我”是否也点赞或收藏过）
    public List<BlogVO> getUsersLikedFavoriteBlogs(Long userId) {
        Claims claims = ThreadLocalUtil.get();
        Long myUserId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        List<BlogVO> blogVOList = new ArrayList<>();

        for (Blog b : blogList) {
            OtherUserVO user = userServiceImpl.getOtherUserInfo(b.getUserId());

            // 查看该用户是否点赞或收藏了该帖子
            Integer isLiked = blogMapper.isLiked(userId, b.getId());
            Integer isFavorite = blogMapper.isFavorite(userId, b.getId());

            // 查看我是否点赞或收藏了该帖子
            Integer isLikedByMe = isLiked;
            Integer isFavoriteByMe = isFavorite;
            if (!userId.equals(myUserId)) {
                isLikedByMe = blogMapper.isLiked(myUserId, b.getId());
                isFavoriteByMe = blogMapper.isFavorite(myUserId, b.getId());
            }

            if (isLiked == 1 || isFavorite == 1) {
                BlogVO blogVO = BlogVO.builder()
                        .id(b.getId())
                        .userId(b.getUserId())
                        .username(user.getUsername())
                        .profilePicture(user.getProfilePicture())
                        .title(b.getTitle())
                        .content(b.getContent())
                        .images(b.getImages())
                        .tags(b.getTags())
                        .likes(b.getLikes())
                        .favorites(b.getFavorites())
                        .isLiked(isLikedByMe)
                        .IsFavorite(isFavoriteByMe)
                        .otherUserLiked(isLiked)
                        .otherUserFavorite(isFavorite)
                        .comments(b.getComments())
                        .createdTime(b.getCreatedTime())
                        .updatedTime(b.getUpdatedTime())
                        .build();
                blogVOList.add(blogVO);
            }
        }
        return blogVOList;
    }

    @Override
    @Transactional
    public void likeBlog(Long blogId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());
        Integer isLiked = blogMapper.isLiked(userId, blogId);

        if (isLiked == 0) {
            blogMapper.DoLikeBlog(userId, blogId);
            blogMapper.increaseLikeNum(blogId);
        } else {
            blogMapper.CancelLikeBlog(userId, blogId);
            blogMapper.decreaseLikeNum(blogId);
        }

        // 更新redis中的数据
        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        for (int i = 0; i < blogList.size(); i++) {
            Blog blog = blogList.get(i);
            if (blogId.equals(blog.getId())) {
                if (isLiked == 0) {
                    blog.setLikes(blog.getLikes() + 1);
                } else {
                    blog.setLikes(blog.getLikes() - 1);
                }
                blogList.set(i, blog);
                break;
            }
        }
        redisUtil.set(blogsKey, blogList, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
    }

    @Override
    @Transactional
    public void favoriteBlog(Long blogId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());
        Integer isFavorite = blogMapper.isFavorite(userId, blogId);

        if (isFavorite == 0) {
            blogMapper.DoFavoriteBlog(userId, blogId);
            blogMapper.increaseFavoriteNum(blogId);
        } else {
            blogMapper.CancelFavoriteBlog(userId, blogId);
            blogMapper.decreaseFavoriteNum(blogId);
        }

        // 更新redis中的数据
        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        for (int i = 0; i < blogList.size(); i++) {
            Blog blog = blogList.get(i);
            if (blogId.equals(blog.getId())) {
                if (isFavorite == 0) {
                    blog.setFavorites(blog.getFavorites() + 1);
                } else {
                    blog.setFavorites(blog.getFavorites() - 1);
                }
                blogList.set(i, blog);
                break;
            }
        }
        redisUtil.set(blogsKey, blogList, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
    }

    @Override
    public void postBlogComment(BlogCommentsDTO blogCommentsDTO) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        BlogComments blogComments = new BlogComments();
        BeanUtils.copyProperties(blogCommentsDTO, blogComments);

        blogComments.setUserId(userId);
        blogComments.setLikes(0);
        blogComments.setStatus(BlogCommentsConstant.NORMAL_STATUS);
        blogComments.setCreateTime(LocalDateTime.now());
        blogComments.setUpdateTime(LocalDateTime.now());

        blogMapper.postBlogComment(blogComments);

        // 更新redis
        String blogCommentsKey = SystemConstant.REDIS_BLOG_COMMENTS_KEY;
        String blogChildrenCommentsKey = SystemConstant.REDIS_BLOG_CHILDREN_COMMENTS_KEY;

        List<BlogComments> blogCommentsList;
        if (blogComments.getParentId() == 0) {
            blogCommentsList = redisUtil.queryListWithCachePenetration(blogCommentsKey, blogComments.getBlogId(), BlogComments.class, this::getParentBlogCommentsFromDB, null, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            if (blogCommentsList == null) {
                blogCommentsList = new ArrayList<>();
            }
            blogCommentsList.add(blogComments);
            redisUtil.set(blogCommentsKey + blogComments.getBlogId(), blogCommentsList, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
        } else {
            blogCommentsList = redisUtil.queryListWithCachePenetration(blogChildrenCommentsKey, blogComments.getBlogId(), blogComments.getParentId(), BlogComments.class, this::getChildrenBlogCommentsFromDB, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            if (blogCommentsList == null) {
                blogCommentsList = new ArrayList<>();
            }
            blogCommentsList.add(blogComments);
            redisUtil.set(blogChildrenCommentsKey + blogComments.getBlogId() + "_" + blogComments.getParentId(), blogCommentsList, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
        }
    }

    public List<Blog> getBlogsFromDB() {
        return blogMapper.getBlogs();
    }

    public List<BlogComments> getParentBlogCommentsFromDB(Long blogId) {
        return blogMapper.getBlogParentComments(blogId);
    }

    public List<BlogComments> getChildrenBlogCommentsFromDB(Long blogId, Long parentId) {
        return blogMapper.getBlogChildrenComments(blogId, parentId);
    }

}
