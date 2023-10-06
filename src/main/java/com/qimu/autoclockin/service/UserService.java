package com.qimu.autoclockin.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.qimu.autoclockin.model.dto.user.UserBindEmailRequest;
import com.qimu.autoclockin.model.dto.user.UserUnBindEmailRequest;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: QiMu
 * @Date: 2023/10/06 09:45:52
 * @Version: 1.0
 * @Description: 用户服务
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param userName      用户名
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String userName);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      要求
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取登录用户
     * 获取当前登录用户
     *
     * @param request 要求
     * @return {@link User}
     */
    User getLoginUser(HttpServletRequest request);
    /**
     * 用户绑定电子邮件
     *
     * @param userEmailLoginRequest 用户电子邮件登录请求
     * @param request               要求
     * @return {@link UserVO}
     */
    User userBindEmail(UserBindEmailRequest userEmailLoginRequest, HttpServletRequest request);

    /**
     * 用户取消绑定电子邮件
     *
     * @param request                要求
     * @param userUnBindEmailRequest 用户取消绑定电子邮件请求
     * @return {@link UserVO}
     */
    User userUnBindEmail(UserUnBindEmailRequest userUnBindEmailRequest, HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request 要求
     * @return boolean
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request 要求
     * @return boolean
     */
    boolean userLogout(HttpServletRequest request);
}
