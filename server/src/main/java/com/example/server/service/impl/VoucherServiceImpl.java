package com.example.server.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONString;
import cn.hutool.json.JSONUtil;
import com.example.common.constant.JwtClaimsConstant;
import com.example.common.constant.SystemConstant;
import com.example.common.constant.VoucherOrderConstant;
import com.example.common.exception.EventEndedException;
import com.example.common.exception.EventYet2StartException;
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
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Override
    public List<VoucherInfoVO> getVouchers() {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        String key = SystemConstant.REDIS_VOUCHER_INFO_KEY;
        List<VoucherInfoVO> voucherInfoVOList = redisUtil.queryListWithCacheBreakdown(key, null, VoucherInfoVO.class, null, this::getVoucherInfoFromDB, SystemConstant.REDIS_VOUCHER_INFO_EXPIRATION, TimeUnit.MINUTES);
        if (voucherInfoVOList == null) {
            throw new RuntimeException();
        }

        // 依次当前用户是否已领取优惠券
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
    public void pickupVoucher(Long voucherId) {
        Claims claims = ThreadLocalUtil.get();
        Long userId = Long.valueOf(claims.get(JwtClaimsConstant.ID).toString());

        // 通过synchronized解决用户一次性可以领取多张同一个优惠券的情况，保证一个优惠券每个用户只能领一张
        synchronized (userId.toString().intern()) {
            // 获取与事务相关的代理对象，确保saveUserVoucher的事务回滚可以在pickupVoucher函数中生效
            VoucherService proxy = (VoucherService) AopContext.currentProxy();
            proxy.saveUserVoucher(voucherId, userId);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUserVoucher(Long voucherId, Long userId) {
        Voucher voucher = voucherMapper.getVoucherById(voucherId);
        UserVoucher userVoucher = new UserVoucher();

        // 检查该用户是否已有该优惠券
        UserVoucher existedUserVoucher = voucherMapper.getUserVoucher(voucherId, userId);
        if (existedUserVoucher != null) {
            throw new UserVoucherExist("您已有该优惠券，不可重复领取！");
        }


        // 如果是秒杀优惠券，还需要让其库存减1
        if (voucher.getType() == 1) {
            SeckillVoucher seckillVoucher = voucherMapper.getSeckillVoucherById(voucherId);
            if (seckillVoucher.getSeckillBeginTime().isAfter(LocalDateTime.now())) {
                throw new EventYet2StartException("秒杀活动尚未开始！请耐心等待哦");
            }
            if (seckillVoucher.getSeckillEndTime().isBefore(LocalDateTime.now())) {
                throw new EventEndedException("秒杀活动已经结束！再看看别的优惠吧");
            }
            if (seckillVoucher.getCurrentStock() < 1) {
                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
            }
            Boolean success = voucherMapper.setSeckillVoucherCurrentStock(voucherId);
            if (!success) {
                throw new SeckillVoucherStockEmpty("该优惠券已被抢光");
            }
        }

        Long id = uniqueIDGenerator.generateUniqueID(SystemConstant.REDIS_USER_VOUCHER_KEY);
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
