-- 优惠券id
local voucherId = ARGV[1]
-- 用户id
local userId = ARGV[2]
-- 优惠券类型
local voucherType = ARGV[3]

-- 库存key（value是优惠券的库存）
local stockKey = "pickUpVoucher_stock_" .. voucherId
-- 用户优惠券key（value是领取过该优惠券的用户set，确保该优惠券每名用户只能领取一张）
local userVoucherKey = "pickUpVoucher_userVoucher_" .. voucherId

-- 判断秒杀优惠券库存是否充足，不充足返回1
if (tonumber(voucherType) == 1 and tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

-- 判断该用户是否领取过该优惠券，若领取过返回2
if (redis.call('sismember', userVoucherKey, userId) == 1) then
    return 2
end

-- 如果上面都没有问题，则用户领取优惠券成功；如果是秒杀优惠券，还需要让库存-1
if (tonumber(voucherType) == 1) then
    redis.call('incrby', stockKey, -1)
end

redis.call('sadd', userVoucherKey, userId)

return 0
