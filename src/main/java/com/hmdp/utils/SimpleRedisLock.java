package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";   //true删除uuid的下划线
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private String name;
    private StringRedisTemplate stringRedisTemplate;


    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 获取分布式锁
     *
     * @param timeSec
     * @return
     */
    @Override
    public boolean tryLock(Long timeSec) {
        //获取当前线程id作为value
        long threadId = Thread.currentThread().getId();
        //UUID用于判断是否是同一用户，threadId判断是否同一线程
        String lockId = ID_PREFIX + threadId;
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, lockId, timeSec, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    /**
     * 解锁
     */
    @Override
    public void unLock() {
        //调用Lua脚本，解决线程安全问题
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getId()
        );
    }
//    @Override
//    public void unLock() {
//        //获取当前lockId
//        long threadId = Thread.currentThread().getId();
//        String lockId = ID_PREFIX + threadId;
//
//        //获取Redis里锁的Id
//        String redisLockId = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
//        //判断是否是同一把锁
//        if (lockId.equals(redisLockId)) {
//            //是同一把锁，才解锁
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}
