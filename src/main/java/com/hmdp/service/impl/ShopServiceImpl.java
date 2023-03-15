package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.netty.util.internal.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 使用Redis缓存商家数据
     *
     * @param id
     */
    @Override
    public Result queryById(Long id) {
        //缓存穿透解决方案
//        Shop shop = queryWithPassThrough(id);

        //缓存击穿解决方案
        Shop shop = queryWithMutex(id);
        if (shop == null) {
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

    /**
     * 缓存击穿解决方案
     *
     * @param id
     * @return
     */
    public Shop queryWithMutex(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1. 查看redis是否有数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        if (StrUtil.isNotBlank(shopJson)) {
            //2. 有，返回数据
            //返回前将Json序列化为对象
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //判断命中的是否是空值
        if (shopJson != null) {
            //返回错误信息
            return null;
        }

        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop;
        try {
            boolean isLock = tryLock(lockKey);

            if (isLock) {
                //如果未获得锁
                Thread.sleep(50);
                //递归循环获取锁
                return queryWithMutex(id);
            }

            //获得锁，查数据库
            shop = this.getById(id);
            if (shop == null) {
                // 先将不存在id存入Redis空值，防止缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存储前将对象序列化为Json
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            //释放锁
            unLock(lockKey);
        }
        return shop;
    }

    /**
     * 缓存商家数据（缓存穿透版）
     *
     * @param id
     * @return
     */
    public Shop queryWithPassThrough(Long id) {
        String key = CACHE_SHOP_KEY + id;
        //1. 查看redis是否有数据
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        //isBlank:空、长度为0、字符串为回车空格等情况为true
        //isEmpty：空、长度为0等情况为true
        if (StrUtil.isNotBlank(shopJson)) {
            //2. 有，返回数据
            //返回前将Json序列化为对象
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        //判断命中的是否是空值
        if (shopJson != null) {
            //不等于null，就一定是空字符串了，返回错误信息
            return null;
        }

        //3. 没有，查数据库
        Shop shop = this.getById(id);

        if (shop == null) {
            // 先将不存在id存入Redis空值，防止缓存穿透
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            //5. 没有，返回404
            return null;
        }
        //4. 数据库有数据，存储给Redis，并返回
        //存储前将对象序列化为Json
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

        return shop;
    }

    /**
     * 获取互斥锁（通过Redis的SetNX实现）
     *
     * @return
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.MINUTES);
        //进行拆箱的时候可能会有空指针异常，所以用工具包判断一下
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁
     *
     * @param key
     */
    private void unLock(String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 更新表，加入事务
     *
     * @param shop
     * @return
     */
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            Result.fail("id不能为空！");
        }
        //1. 先更数据库
        this.updateById(shop);

        //2. 再删Redis缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }
}
