-- userVoucher的id
local id = ARGV[1]
-- 优惠券id
local voucherId = ARGV[2]
-- 用户id
local userId = ARGV[3]
-- 优惠券类型
local voucherType = ARGV[4]

-- 库存key（value是优惠券的库存）
local stockKey = "pickUpVoucher_stock_" .. voucherId
-- 用户优惠券key（value是领取过该优惠券的用户set，确保该优惠券每名用户只能领取一张）
local userVoucherKey = "pickUpVoucher_userVoucher_" .. voucherId

-- 判断秒杀优惠券库存是否充足，不充足返回1
if (tonumber(voucherType) == 1 and tonumber(redis.call('get', stockKey)) <= 0) then
    return 1
end

-- 判断该用户是否领取过该优惠券（即userId中是否在set集合中），若领取过返回2
if (redis.call('sismember', userVoucherKey, userId) == 1) then
    return 2
end

-- 如果上面都没有问题，则用户领取优惠券成功；如果是秒杀优惠券，还需要让库存-1
if (tonumber(voucherType) == 1) then
    redis.call('incrby', stockKey, -1) -- 库存-1
end

redis.call('sadd', userVoucherKey, userId) -- 将该用户id添加到set集合中

-- 向消息队列中发送消息（相当于保存了userVoucher的信息）
-- 消息队列名称为stream_userVouchers，*表示自动生成具有唯一表示的消息id，后面跟的是消息队列的内容{k1:v1,k2:v2,...}
redis.call('xadd', 'stream_userVouchers', '*', 'id', id, 'voucherId', voucherId, 'userId', userId, 'type', voucherType)

return 0
