package com.example.server.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.exception.ExpireCodeException;
import com.example.common.exception.IncorrectCodeException;
import com.example.common.exception.InvalidPhoneNumberException;
import com.example.common.exception.NoDataInDBException;
import com.example.common.properties.CodeProperties;
import com.example.common.properties.JwtProperties;
import com.example.common.utils.JwtUtil;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.common.utils.ValidateRegExpUtil;
import com.example.pojo.dto.UserLoginDTO;
import com.example.pojo.entity.User;
import com.example.pojo.entity.UserInfo;
import com.example.pojo.vo.OtherUserVO;
import com.example.pojo.vo.UserLoginVO;
import com.example.pojo.vo.UserVO;
import com.example.server.mapper.UserInfoMapper;
import com.example.server.mapper.UserMapper;
import com.example.server.service.UserService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@SpringBootApplication(scanBasePackages = "com.example.common")
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CodeProperties codeProperties;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private FollowServiceImpl followServiceImpl;

    @Override
    public void getCode(String phoneNumber) {
        // 验证手机号是否合法
        if (!ValidateRegExpUtil.isValidPhoneNumber(phoneNumber)) {
            throw new InvalidPhoneNumberException("手机号格式不合法！");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(4);

        // 将验证码保存在redis中
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set(phoneNumber, code, codeProperties.getExpiration(), TimeUnit.MILLISECONDS);

        // 发送验证码
        System.out.println("验证码发送成功：" + code);
    }

    @Override
    public UserLoginVO login(UserLoginDTO userLoginDTO) {
        // 验证手机号是否合法
        if (!ValidateRegExpUtil.isValidPhoneNumber(userLoginDTO.getPhoneNumber())) {
            throw new InvalidPhoneNumberException("手机号格式不合法！");
        }

        // 验证验证码
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String code = ops.get(userLoginDTO.getPhoneNumber());
        if (code == null) {
            throw new ExpireCodeException("验证码已过期！");
        }
        if (!code.equals(userLoginDTO.getCode())) {
            throw new IncorrectCodeException("验证码错误！");
        }

        // 查找用户是否已注册，如果没有注册，则自动进行注册
        User user = userMapper.findUserByPhoneNumber(userLoginDTO.getPhoneNumber());
        if (user == null) {
            user = new User();
            UserInfo userInfo = new UserInfo();
            BeanUtils.copyProperties(userLoginDTO, user);
            String username = SystemConstant.USERNAME_PREFIX + RandomUtil.randomString(10);
            user.setUsername(username);
            userMapper.register(user);

            userInfo.setUserId(user.getId());
            userInfo.setFans(0);
            userInfo.setFollowee(0);
            userInfo.setCredits(0);
            userInfo.setLevel(0);
            userInfoMapper.addUserInfo(userInfo);
        }

        // 获取用户的关注和粉丝信息
        followServiceImpl.getMyFolloweeFans(user.getId());

        // 生成token并保存在redis中
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.ID, user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getExpiration(), claims);
        ops.set(token, token, jwtProperties.getExpiration(), TimeUnit.MILLISECONDS);

        // 返回数据
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .profilePicture(user.getProfilePicture())
                .token(token)
                .build();
        return userLoginVO;
    }

    @Override
    public UserVO getUserInfo() {
        Claims claims = ThreadLocalUtil.get();
        Long id = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        User user = this.getUserFromCache(id);

        UserInfo userInfo = this.getUserInfoFromCache(id);

        // 计算年龄
        Integer age = this.calculateAge(userInfo);

        UserVO userVO = UserVO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .username(user.getUsername())
                .profilePicture(user.getProfilePicture())
                .city(userInfo.getCity())
                .introduction(userInfo.getIntroduction())
                .fans(userInfo.getFans())
                .followee(userInfo.getFollowee())
                .gender(userInfo.getGender())
                .age(age)
                .school(userInfo.getSchool())
                .credits(userInfo.getCredits())
                .level(userInfo.getLevel())
                .build();

        return userVO;
    }

    @Override
    public OtherUserVO getOtherUserInfo(Long id) {
        User user = this.getUserFromCache(id);

        UserInfo userInfo = this.getUserInfoFromCache(id);

        // 计算年龄
        Integer age = this.calculateAge(userInfo);

        OtherUserVO otherUserVO = OtherUserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profilePicture(user.getProfilePicture())
                .city(userInfo.getCity())
                .introduction(userInfo.getIntroduction())
                .fans(userInfo.getFans())
                .followee(userInfo.getFollowee())
                .gender(userInfo.getGender())
                .age(age)
                .school(userInfo.getSchool())
                .level(userInfo.getLevel())
                .build();

        return otherUserVO;
    }

    public User getUserByIdFromDB(Long userId) {
        return userMapper.getUserById(userId);
    }

    public UserInfo getUserInfoByIdFromDB(Long userId) {
        return userInfoMapper.getUserInfoByUserId(userId);
    }

    public User getUserFromCache(Long userId) {
        User user = redisUtil.queryWithCachePenetration(SystemConstant.REDIS_USER_KEY, userId, User.class, this::getUserByIdFromDB, null, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES).getData();
        if (user == null) {
            throw new NoDataInDBException("该用户不存在！");
        }
        return user;
    }

    public UserInfo getUserInfoFromCache(Long userId) {
        UserInfo userInfo = redisUtil.queryWithCachePenetration(SystemConstant.REDIS_USER_INFO_KEY, userId, UserInfo.class, this::getUserInfoByIdFromDB, null, SystemConstant.REDIS_USER_EXPIRATION, TimeUnit.MINUTES).getData();
        if (userInfo == null) {
            throw new NoDataInDBException("该用户信息数据不存在！");
        }
        return userInfo;
    }

    public Integer calculateAge(UserInfo userInfo) {
        LocalDate now = LocalDate.now();
        Integer age = null;
        if (userInfo.getBirthday() != null) {
            age = userInfo.getBirthday().until(now).getYears();
        }
        return age;
    }
}
