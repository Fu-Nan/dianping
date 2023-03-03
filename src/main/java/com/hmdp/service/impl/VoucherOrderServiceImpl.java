package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisWorker redisWorker;

    /**
     * 秒杀优惠券下单
     *
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        //1. 查询优惠券信息
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);

        //2. 判断秒杀是否开始
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime beginTime = seckillVoucher.getBeginTime();
        LocalDateTime endTime = seckillVoucher.getEndTime();
        if (beginTime.isAfter(now)) {
            //3. 否，返回异常结果
            return Result.fail("抢购时间未到！");
        }
        if (endTime.isBefore(now)) {
            return Result.fail("抢购时间已结束！");
        }

        //4. 是，判断库存是否充足
        if (seckillVoucher.getStock() < 1) {
            //5. 否，返回异常结果
            return Result.fail("抢购完了，下次再来吧");
        }

        Long userId = UserHolder.getUser().getId();
        //上锁，用户id当锁，这样就实现一个用户一把锁
        synchronized (userId.toString().intern()) {
            //获取代理对象（事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    /**
     * 封装用户下单时的操作
     *
     * @param voucherId
     * @return
     */
    public Result createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        //根据优惠券和用于id查询订单
        int count = this.query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            //如果用户已经下过单
            return Result.fail("同一用户只能下一单！");
        }

        //6. 是，扣减库存
//        seckillVoucher.setStock(seckillVoucher.getStock() - 1);
//        seckillVoucherService.updateById(seckillVoucher);
        //采用乐观锁，解决超卖问题
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足!");
        }
        //7. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //7.1 订单id
        long orderId = redisWorker.nextId("order");
        voucherOrder.setId(orderId);
        //7.2 用户id
        voucherOrder.setUserId(userId);
        //7.3 优惠券id
        voucherOrder.setVoucherId(voucherId);
        this.save(voucherOrder);
        //8. 返回订单id
        return Result.ok(orderId);
    }
}
