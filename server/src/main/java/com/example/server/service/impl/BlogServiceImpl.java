package com.example.server.service.impl;

import com.example.common.constant.BlogCommentsConstant;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.PaginationConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.exception.NoDataInDBException;
import com.example.common.result.PaginationResult;
import com.example.common.result.QueryRedisListResult;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.pojo.dto.BlogCommentsDTO;
import com.example.pojo.dto.BlogDTO;
import com.example.pojo.entity.Blog;
import com.example.pojo.entity.BlogComments;
import com.example.pojo.vo.*;
import com.example.server.mapper.BlogMapper;
import com.example.server.service.BlogService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class BlogServiceImpl implements BlogService {
    @Autowired
    private BlogMapper blogMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UserServiceImpl userServiceImpl;
    @Autowired
    private FollowServiceImpl followServiceImpl;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<BlogVO> getBlogs() {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES).getData();
        List<BlogVO> blogVOList = new ArrayList<>();
        for (Blog b : blogList) {
            OtherUserVO otherUserVO = userServiceImpl.getOtherUserInfo(b.getUserId());
            Integer isLiked = blogMapper.isLiked(userId, b.getId());
            Integer isFavorite = blogMapper.isFavorite(userId, b.getId());

            BlogVO blogVO = BlogVO.builder().id(b.getId()).userId(b.getUserId()).username(otherUserVO.getUsername()).profilePicture(otherUserVO.getProfilePicture()).title(b.getTitle()).content(b.getContent()).images(b.getImages()).tags(b.getTags()).address(b.getAddress()).likes(b.getLikes()).favorites(b.getFavorites()).isLiked(isLiked).IsFavorite(isFavorite).comments(b.getComments()).createTime(b.getCreateTime()).updateTime(b.getUpdateTime()).build();
            blogVOList.add(blogVO);
        }
        return blogVOList;
    }

    @Override
    public BlogContentVO getBlogContent(Long blogId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES).getData();
        BlogVO blogVO = new BlogVO();
        for (Blog b : blogList) {
            if (blogId.equals(b.getId())) {
                OtherUserVO otherUserVO = userServiceImpl.getOtherUserInfo(b.getUserId());

                Integer isLiked = blogMapper.isLiked(userId, b.getId());
                Integer isFavorite = blogMapper.isFavorite(userId, b.getId());

                blogVO = BlogVO.builder().id(b.getId()).userId(b.getUserId()).username(otherUserVO.getUsername()).profilePicture(otherUserVO.getProfilePicture()).title(b.getTitle()).content(b.getContent()).images(b.getImages()).tags(b.getTags()).address(b.getAddress()).likes(b.getLikes()).favorites(b.getFavorites()).isLiked(isLiked).IsFavorite(isFavorite).comments(b.getComments()).createTime(b.getCreateTime()).updateTime(b.getUpdateTime()).build();
                break;
            }
        }

        String blogCommentsKey = SystemConstant.REDIS_BLOG_COMMENTS_KEY;
        String blogChildrenCommentsKey = SystemConstant.REDIS_BLOG_CHILDREN_COMMENTS_KEY;
        List<BlogComments> blogCommentsList = redisUtil.queryListWithCachePenetration(blogCommentsKey, blogId, BlogComments.class, this::getParentBlogCommentsFromDB, null, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES).getData();
        List<BlogCommentsVO> blogCommentsVOList = new ArrayList<>();

        for (BlogComments b : blogCommentsList) {
            OtherUserVO otherUserVO = userServiceImpl.getOtherUserInfo(b.getUserId());

            // 获取二级评论
            List<BlogComments> blogChildrenCommentsList = redisUtil.queryListWithCachePenetration(blogChildrenCommentsKey, blogId, b.getId(), BlogComments.class, this::getChildrenBlogCommentsFromDB, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES).getData();
            List<BlogCommentsVO> blogChildrenCommentsVOList = new ArrayList<>();

            if (blogChildrenCommentsList != null && !blogChildrenCommentsList.isEmpty()) {
                for (BlogComments cb : blogChildrenCommentsList) {
                    OtherUserVO postUserVO = userServiceImpl.getOtherUserInfo(cb.getUserId());
                    OtherUserVO replyUserVO = new OtherUserVO();
                    if (cb.getReplyId() != 0) {
                        replyUserVO = userServiceImpl.getOtherUserInfo(cb.getReplyUserId());
                    }
                    BlogCommentsVO blogCommentsVO = BlogCommentsVO.builder().id(cb.getId()).blogId(cb.getBlogId()).userId(cb.getUserId()).parentId(cb.getParentId()).replyId(cb.getReplyId()).replyUserId(cb.getReplyUserId()).replyUsername(replyUserVO.getUsername()).username(postUserVO.getUsername()).profilePicture(postUserVO.getProfilePicture()).content(cb.getContent()).likes(cb.getLikes()).status(cb.getStatus()).createTime(cb.getCreateTime()).updateTime(cb.getUpdateTime()).build();
                    blogChildrenCommentsVOList.add(blogCommentsVO);
                }
            }

            BlogCommentsVO blogCommentsVO = BlogCommentsVO.builder().id(b.getId()).blogId(b.getBlogId()).userId(b.getUserId()).parentId(b.getParentId()).replyId(b.getReplyId()).username(otherUserVO.getUsername()).profilePicture(otherUserVO.getProfilePicture()).content(b.getContent()).likes(b.getLikes()).status(b.getStatus()).childrenComments(blogChildrenCommentsVOList).createTime(b.getCreateTime()).updateTime(b.getUpdateTime()).build();
            blogCommentsVOList.add(blogCommentsVO);
        }

        BlogContentVO blogContentVO = BlogContentVO.builder().blog(blogVO).comments(blogCommentsVOList).build();
        return blogContentVO;
    }

    @Override
    public List<BlogVO> getUsersBlogs(Long userId) {
        Claims claims = ThreadLocalUtil.get();
        Long myUserId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES).getData();
        List<BlogVO> blogVOList = new ArrayList<>();

        OtherUserVO user = userServiceImpl.getOtherUserInfo(userId);
        for (Blog b : blogList) {
            if (b.getUserId().equals(userId)) {
                Integer isLiked = blogMapper.isLiked(myUserId, b.getId());
                Integer isFavorite = blogMapper.isFavorite(myUserId, b.getId());

                BlogVO blogVO = BlogVO.builder().id(b.getId()).userId(b.getUserId()).username(user.getUsername()).profilePicture(user.getProfilePicture()).title(b.getTitle()).content(b.getContent()).images(b.getImages()).tags(b.getTags()).address(b.getAddress()).likes(b.getLikes()).favorites(b.getFavorites()).isLiked(isLiked).IsFavorite(isFavorite).comments(b.getComments()).createTime(b.getCreateTime()).updateTime(b.getUpdateTime()).build();
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
        List<Blog> blogList = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES).getData();
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
                BlogVO blogVO = BlogVO.builder().id(b.getId()).userId(b.getUserId()).username(user.getUsername()).profilePicture(user.getProfilePicture()).title(b.getTitle()).content(b.getContent()).images(b.getImages()).tags(b.getTags()).address(b.getAddress()).likes(b.getLikes()).favorites(b.getFavorites()).isLiked(isLikedByMe).IsFavorite(isFavoriteByMe).otherUserLiked(isLiked).otherUserFavorite(isFavorite).comments(b.getComments()).createTime(b.getCreateTime()).updateTime(b.getUpdateTime()).build();
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
        // 如果redis中的数据没有过期，则更新
        // 否则会执行queryListWithCachePenetration中的数据库查询操作，返回的就是新数据，flag就为true，不会执行更新操作
        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        QueryRedisListResult<Blog> q = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);

        if (!q.getFlag()) {
            List<Blog> blogList = q.getData();
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
        QueryRedisListResult<Blog> q = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);

        if (!q.getFlag()) {
            List<Blog> blogList = q.getData();
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
        blogMapper.updateBlogCommentNum(blogComments.getBlogId());

        // 更新redis
        String blogCommentsKey = SystemConstant.REDIS_BLOG_COMMENTS_KEY;
        String blogChildrenCommentsKey = SystemConstant.REDIS_BLOG_CHILDREN_COMMENTS_KEY;

        // 更新redis中的笔记评论数量
        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        QueryRedisListResult<Blog> q1 = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        if (!q1.getFlag()) {
            List<Blog> blogList = q1.getData();
            for (int i = 0; i < blogList.size(); i++) {
                if (blogList.get(i).getId().equals(blogComments.getBlogId())) {
                    Blog blog = blogList.get(i);
                    Integer comments = blogList.get(i).getComments() + 1;
                    blog.setComments(comments);
                    blogList.set(i, blog);
                    break;
                }
            }
            redisUtil.set(blogsKey, blogList, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        }

        // 更新redis中的笔记评论
        if (blogComments.getParentId() == 0) {
            QueryRedisListResult<BlogComments> q2 = redisUtil.queryListWithCachePenetration(blogCommentsKey, blogComments.getBlogId(), BlogComments.class, this::getParentBlogCommentsFromDB, null, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            if (!q2.getFlag()) {
                List<BlogComments> blogCommentsList = q2.getData();
                if (blogCommentsList == null) {
                    blogCommentsList = new ArrayList<>();
                }
                blogCommentsList.add(blogComments);
                redisUtil.set(blogCommentsKey + blogComments.getBlogId(), blogCommentsList, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            }

        } else {
            QueryRedisListResult<BlogComments> q2 = redisUtil.queryListWithCachePenetration(blogChildrenCommentsKey, blogComments.getBlogId(), blogComments.getParentId(), BlogComments.class, this::getChildrenBlogCommentsFromDB, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            if (!q2.getFlag()) {
                List<BlogComments> blogCommentsList = q2.getData();
                if (blogCommentsList == null) {
                    blogCommentsList = new ArrayList<>();
                }
                blogCommentsList.add(blogComments);
                redisUtil.set(blogChildrenCommentsKey + blogComments.getBlogId() + "_" + blogComments.getParentId(), blogCommentsList, SystemConstant.REDIS_BLOG_COMMENTS_EXPIRATION, TimeUnit.MINUTES);
            }
        }
    }

    @Override
    public void postBlog(BlogDTO blogDTO) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        Blog blog = new Blog();
        BeanUtils.copyProperties(blogDTO, blog);

        blog.setUserId(userId);
        blog.setLikes(0);
        blog.setFavorites(0);
        blog.setComments(0);
        blog.setCreateTime(LocalDateTime.now());
        blog.setUpdateTime(LocalDateTime.now());

        blogMapper.postBlog(blog);

        // 更新redis
        String blogsKey = SystemConstant.REDIS_BLOGS_KEY;
        QueryRedisListResult<Blog> q = redisUtil.queryListWithCachePenetration(blogsKey, null, Blog.class, null, this::getBlogsFromDB, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        if (!q.getFlag()) {
            List<Blog> blogList = q.getData();
            blogList.add(blog);
            redisUtil.set(blogsKey, blogList, SystemConstant.REDIS_BLOGS_EXPIRATION, TimeUnit.MINUTES);
        }

        // 推送笔记id给所有粉丝（还包含时间戳作为标识）
        List<FollowUserVO> list = followServiceImpl.getFans(userId);
        for (FollowUserVO followUserVO : list) {
            String feedStreamKey = SystemConstant.REDIS_FEED_STREAM_KEY + followUserVO.getUserId();
            stringRedisTemplate.opsForZSet().add(feedStreamKey, blog.getId().toString(), System.currentTimeMillis());
        }
    }

    @Override
    // 分页查询所关注用户的最新笔记动态
    public PaginationResult<BlogVO> getFolloweeBlogs(Long max, Integer offset) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String key = SystemConstant.REDIS_FEED_STREAM_KEY + userId;

        // 解析数据：包含blogId和时间戳，并得到minTime（最后一个blogId的时间戳）和offset（具有相同mintTime的blogId的个数）
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, PaginationConstant.DEFAULT_PAGE_SIZE);
        if (typedTuples == null || typedTuples.isEmpty()) {
            return null;
        }

        Long minTime = 0L;
        Integer newOffset = 1;
        List<Long> blogIds = new ArrayList<>(typedTuples.size());
        for (ZSetOperations.TypedTuple<String> typedTuple : typedTuples) {
            // 获取blogId并添加到列表中
            blogIds.add(Long.valueOf(Objects.requireNonNull(typedTuple.getValue())));
            // 获取分数（时间戳）
            long score = typedTuple.getScore().longValue();

            if (score == minTime) {
                newOffset++;
            } else {
                minTime = score;
                newOffset = 1;
            }
        }

        List<BlogVO> blogVOList = getBlogs();
        List<BlogVO> followeeBlogVOList = new ArrayList<>();
        for (BlogVO b : blogVOList) {
            if (blogIds.contains(b.getId())) {
                followeeBlogVOList.add(b);
            }
        }
        return new PaginationResult<>(followeeBlogVOList, minTime, newOffset);
    }

    @Override
    public List<BlogVO> getNearbyBlogs(Integer page) {
        Integer fromIndex = (page - 1) * PaginationConstant.DEFAULT_PAGE_SIZE;
        Integer count = page * PaginationConstant.DEFAULT_PAGE_SIZE;

        Double myLocationX = 121.435312;
        Double myLocationY = 31.197414;
        // GeoReference.fromCoordinate(myLocationX,myLocationY)表示以‘我’的经纬度坐标为圆心
        // new Distance(10000)表示寻找定位‘我’附近在10km以内的笔记
        /*RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(page * 4)
        表示返回结果带上每一篇笔记的定位离‘我’的距离，同时查询数量限定在第一篇到第count篇
        注：limit的参数是查询的数量*/
        GeoResults<RedisGeoCommands.GeoLocation<String>> searchResult = stringRedisTemplate.opsForGeo().search(
                SystemConstant.REDIS_GEO_BLOGS_KEY,
                GeoReference.fromCoordinate(myLocationX, myLocationY),
                new Distance(10000),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(count)
        );

        if (searchResult == null) {
            throw new NoDataInDBException("暂无数据");
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = searchResult.getContent();

        List<Long> ids = new ArrayList<>(content.size());
        // stream().skip(index)是从index开始截取的
        content.stream().skip(fromIndex).forEach(result -> {
            Long id = Long.valueOf(result.getContent().getName());
            ids.add(id);
        });

        List<BlogVO> blogList = getBlogs();
        List<BlogVO> nearbyBlogVOList = new ArrayList<>();
        for (BlogVO b : blogList) {
            if (ids.contains(b.getId())) {
                nearbyBlogVOList.add(b);
            }
        }
        return nearbyBlogVOList;
    }

    public void loadBlogsGeo() {
        List<Blog> list = getBlogsFromDB();
        for (Blog blog : list) {
            stringRedisTemplate.opsForGeo().add(SystemConstant.REDIS_GEO_BLOGS_KEY, new Point(blog.getLocationX(), blog.getLocationY()), blog.getId().toString());
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
