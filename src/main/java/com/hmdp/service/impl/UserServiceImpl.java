package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 发送验证码
     *
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //2. 手机号格式不符合
            return Result.fail("手机号格式错误！");
        }
        //3. 手机号格式符合
        //4. 生成验证码
        String code = RandomUtil.randomNumbers(6);
        //5. 保存验证码
        session.setAttribute("code", code);
        //6. 发送验证码
        log.debug("短信验证码发送成功，验证码为：{}", code);
        return Result.ok();
    }

    /**
     * 用户登录
     *
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        //先再次校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            //手机号格式不符合
            return Result.fail("手机号格式错误！");
        }

        //1. 校验验证码
        String cacheCode = (String) session.getAttribute("code");
        if (cacheCode == null || !loginForm.getCode().equals(cacheCode)) {
            //2. 验证码不一致
            return Result.fail("验证码错误!");
        }

        //3. 验证码一致
        //4. 根据手机号查用户
        User user = this.lambdaQuery().eq(User::getPhone, phone).one();
        if (user == null) {
            //5. 用户不存在，创建新用户，保存用户到数据库
            user = this.createUserWithPhone(phone);
        }

        //将数据封装到DTO去，不然传给前端的无用信息过多
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);

        //6. 保存用户到session
        session.setAttribute("user", userDTO);
        return Result.ok();
    }

    /**
     * 根据phone创建新用户
     *
     * @param phone
     * @return
     */
    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        this.save(user);
        return user;
    }
}
