package com.hmdp.config;

import com.hmdp.utils.interceptor.RefreshTokenInterceptor;
import com.hmdp.utils.interceptor.UserLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {
    @Autowired
    private UserLoginInterceptor userLoginInterceptor;

    @Autowired
    private RefreshTokenInterceptor refreshTokenInterceptor;

    /**
     * 用于Controller层的登录权限控制
     * 拦截器执行顺序默认是书写的顺序，也可以使用.sort(2)设置，数字越大优先级越低
     *
     * @param registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(refreshTokenInterceptor).addPathPatterns("/**");

        //采用排除不需要拦截的请求地址
        registry.addInterceptor(userLoginInterceptor).excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login"
        );
    }
}
