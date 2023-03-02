package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 使用Redis缓存首页店铺分类功能
     *
     * @return
     */
    @Override
    public List<ShopType> queryByList() {

        //1. redis是否有缓存
        Long size = stringRedisTemplate.opsForList().size(CACHE_SHOP_TYPE_KEY);

        //2. 有，直接返回
        if (size != 0) {
            List<String> typeList = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_KEY, 0, size);
            //返回前将List<String>转为List<ShopType>
            List<ShopType> shopTypes = typeList.stream().map(s -> {
                ShopType shopType = JSONUtil.toBean(s, ShopType.class);
                return shopType;
            }).collect(Collectors.toList());
            return shopTypes;
        }

        //3. 没有，查数据库
        List<ShopType> typeList = this.query().orderByAsc("sort").list();

        if (typeList == null || typeList.isEmpty()) {
            //4. 数据库没有，直接返回错误
//            return Result.fail("分类错误！未找到");
            return null;
        }

        //5. 数据库有，使用Redis缓存，并返回
        //返回前将List<ShopType>转为List<String>
        List<String> typeListToStrings = typeList.stream().map(shopType -> {
            return JSONUtil.toJsonStr(shopType);
        }).collect(Collectors.toList());
        stringRedisTemplate.opsForList().leftPushAll(CACHE_SHOP_TYPE_KEY, typeListToStrings);

        return typeList;
    }
}
