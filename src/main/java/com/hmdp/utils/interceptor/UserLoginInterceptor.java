package com.hmdp.utils.interceptor;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 拦截器，拦截用户未登录的部分请求
 */
@Component
public class UserLoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1. 从session获取当前用户
        HttpSession session = request.getSession();
        Object user = session.getAttribute("user");

        //2. 当前用户是否存在
        if (user == null) {
            //3. 不存在，拦截
            response.setStatus(401);
            return false;
        }
        //4. 存在，保存到ThreadLocal并放行
        UserHolder.saveUser((UserDTO) user);
        return true;

    }

    /**
     * 释放ThreadLocal，防止内存泄露
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
