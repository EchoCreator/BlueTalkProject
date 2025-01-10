package com.example.server.service;

import com.example.pojo.vo.VoucherInfoVO;

import java.util.List;

public interface VoucherService {
    List<VoucherInfoVO> getVouchers();

    void pickupVoucher(Long voucherId);
}
