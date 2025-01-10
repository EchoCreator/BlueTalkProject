package com.example.server.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONString;
import cn.hutool.json.JSONUtil;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.constant.VoucherOrderConstant;
import com.example.common.exception.SeckillVoucherStockEmpty;
import com.example.common.exception.UserVoucherExist;
import com.example.common.utils.RedisUtil;
import com.example.common.utils.ThreadLocalUtil;
import com.example.common.utils.UniqueIDGenerator;
import com.example.pojo.entity.SeckillVoucher;
import com.example.pojo.entity.Voucher;
import com.example.pojo.entity.UserVoucher;
import com.example.pojo.vo.RedisDataVO;
import com.example.pojo.vo.VoucherInfoVO;
import com.example.server.mapper.VoucherMapper;
import com.example.server.service.VoucherService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VoucherServiceImpl implements VoucherService {
    @Autowired
    private VoucherMapper voucherMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private UniqueIDGenerator uniqueIDGenerator;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public List<VoucherInfoVO> getVouchers() {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String key = SystemConstant.REDIS_VOUCHER_INFO_KEY;
        List<VoucherInfoVO> voucherInfoVOList = redisUtil.queryListWithCacheBreakdown(key, null, VoucherInfoVO.class, null, this::getVoucherInfoFromDB, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);
        if (voucherInfoVOList == null) {
            throw new RuntimeException();
        }
        for (int i = 0; i < voucherInfoVOList.size(); i++) {
            VoucherInfoVO voucherInfoVO = voucherInfoVOList.get(i);
            UserVoucher userVoucher = voucherMapper.getUserVoucher(voucherInfoVOList.get(i).getId(), userId);
            Integer isPickedUp = 0;
            if (userVoucher != null) {
                isPickedUp = 1;
            }
            voucherInfoVO.setIsPickedUp(isPickedUp);
            voucherInfoVOList.set(i, voucherInfoVO);
        }
        return voucherInfoVOList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pickupVoucher(Long voucherId) {
        Voucher voucher = voucherMapper.getVoucherById(voucherId);
        UserVoucher userVoucher = new UserVoucher();
        Long id = uniqueIDGenerator.generateUniqueID(SystemConstant.REDIS_USER_VOUCHER_KEY);

        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        // 检查该用户是否已有该优惠券
        UserVoucher existedUserVoucher = voucherMapper.getUserVoucher(voucherId, userId);
        if (existedUserVoucher != null) {
            throw new UserVoucherExist("您已有该优惠券，不可重复领取！");
        }


        // 如果是秒杀优惠券，还需要让其库存减1
        if (voucher.getType() == 1) {
            SeckillVoucher seckillVoucher = voucherMapper.getSeckillVoucherById(voucherId);
            if (seckillVoucher.getCurrentStock() < 1) {
                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
            }
            Boolean success = voucherMapper.setSeckillVoucherCurrentStock(voucherId);
            if (!success) {
                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
            }
        }

        userVoucher.setId(id);
        userVoucher.setVoucherId(voucherId);
        userVoucher.setUserId(userId);
        userVoucher.setStatus(VoucherOrderConstant.VOUCHER_STATUS_UNUSED);

        voucherMapper.addUserVoucher(userVoucher);
    }

    public List<VoucherInfoVO> getVoucherInfoFromDB() {
        List<VoucherInfoVO> voucherInfoVOList = new ArrayList<>();
        List<Voucher> voucherList = voucherMapper.getVouchers();
        for (Voucher voucher : voucherList) {
            SeckillVoucher seckillVoucher = new SeckillVoucher();
            if (voucher.getType() == 1) {
                seckillVoucher = voucherMapper.getSeckillVoucherById(voucher.getId());
            }

            VoucherInfoVO voucherInfoVO = VoucherInfoVO.builder()
                    .id(voucher.getId())
                    .shopId(voucher.getShopId())
                    .title(voucher.getTitle())
                    .subTitle(voucher.getSubTitle())
                    .rules(voucher.getRules())
                    .type(voucher.getType())
                    .status(voucher.getStatus())
                    .stock(seckillVoucher.getStock())
                    .seckillBeginTime(seckillVoucher.getSeckillBeginTime())
                    .seckillEndTime(seckillVoucher.getSeckillBeginTime())
                    .expireBeginTime(voucher.getExpireBeginTime())
                    .expireEndTime(voucher.getExpireEndTime())
                    .build();
            voucherInfoVOList.add(voucherInfoVO);
        }
        return voucherInfoVOList;
    }
}
