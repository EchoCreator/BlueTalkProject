package com.example.server.controller.user;

import com.example.common.result.Result;
import com.example.pojo.vo.VoucherInfoVO;
import com.example.server.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user/voucher")
public class VoucherController {
    @Autowired
    private VoucherService voucherService;

    @GetMapping("/getVouchers")
    public Result<List<VoucherInfoVO>> getVouchers() {
        List<VoucherInfoVO> voucherInfoVOList=voucherService.getVouchers();
        return Result.success(voucherInfoVOList);
    }

    @PostMapping("/pickupVoucher")
    public Result pickupVoucher(Long voucherId) {
        voucherService.pickupVoucher(voucherId);
        return Result.success();
    }
}
