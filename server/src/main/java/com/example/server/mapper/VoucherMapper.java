package com.example.server.mapper;

import com.example.pojo.entity.SeckillVoucher;
import com.example.pojo.entity.UserVoucher;
import com.example.pojo.entity.Voucher;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface VoucherMapper {
    @Select("select * from voucher where status=0")
    List<Voucher> getVouchers();

    @Select("select * from voucher where id=#{id}")
    Voucher getVoucherById(Long id);

    @Select("select * from seckill_voucher where voucher_id=#{id}")
    SeckillVoucher getSeckillVoucherById(Long id);

    @Insert("insert into user_voucher(id, user_id, voucher_id, status, create_time, update_time) values" +
            " (#{id},#{userId},#{voucherId},#{status},now(),now())")
    void addUserVoucher(UserVoucher userVoucher);

    @Update("update seckill_voucher set current_stock=current_stock-1 where voucher_id=#{voucherId} and current_stock>0")
    Boolean setSeckillVoucherCurrentStock(Long voucherId);

    @Select("select * from user_voucher where voucher_id=#{voucherId} and user_id=#{userId}")
    UserVoucher getUserVoucher(Long voucherId, Long userId);
}
