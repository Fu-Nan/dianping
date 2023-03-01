package com.hmdp.utils.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;

/**
 * 拦截器，拦截用户未登录的部分请求
 */
@Component
public class UserLoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        //1. 从session获取当前用户
//        HttpSession session = request.getSession();
//        Object user = session.getAttribute("user");
//
//        //2. 当前用户是否存在
//        if (user == null) {
//            //3. 不存在，拦截
//            response.setStatus(401);
//            return false;
//        }
//        //4. 存在，保存到ThreadLocal并放行
//        UserHolder.saveUser((UserDTO) user);
//        return true;

        //判断是否需要拦截当前请求（ThreadLocal中是否有用户）
        if (UserHolder.getUser() == null) {
            //没有用户，直接拦截
            response.setStatus(401);
            return false;
        }
        // 放行
        return true;
    }
}
