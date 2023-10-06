package com.qimu.autoclockin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qimu.autoclockin.model.entity.ClockInInfo;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.vo.ClockInInfoVo;
import com.qimu.autoclockin.model.vo.LoginResultVO;
import com.qimu.autoclockin.utils.AutoSignUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 用户服务测试
 *
 * @author qimu
 */
@SpringBootTest
class UserServiceTest {

    @Resource
    private UserService userService;

    @Resource
    private ClockInInfoService clockInInfoService;

    @Test
    void testAddUser() {
        User user = new User();
        boolean result = userService.save(user);
        System.out.println(user.getId());
        Assertions.assertTrue(result);
    }

    @Test
    void testUpdateUser() {
        User user = new User();
        boolean result = userService.updateById(user);
        Assertions.assertTrue(result);
    }

    @Test
    void testDeleteUser() {
        boolean result = userService.removeById(1L);
        Assertions.assertTrue(result);
    }

    @Test
    void testGetUser() throws InterruptedException {
        User user = userService.getById(1690665894673338370L);
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        clockInInfoVo.setUserAccount(user.getUserAccount());
        clockInInfoVo.setUserPassword(user.getUserPassword());
        LambdaQueryWrapper<ClockInInfo> clockInInfoQueryWrapper = new LambdaQueryWrapper<>();
        clockInInfoQueryWrapper.eq(ClockInInfo::getUserId, user.getId());
        ClockInInfo clockInInfoServiceOne = clockInInfoService.getOne(clockInInfoQueryWrapper);
        clockInInfoVo.setDeviceType(clockInInfoServiceOne.getDeviceType());
        clockInInfoVo.setDeviceId(clockInInfoVo.getDeviceId());
        clockInInfoVo.setLongitude(clockInInfoVo.getLongitude());
        clockInInfoVo.setLatitude(clockInInfoVo.getLatitude());
        clockInInfoVo.setAddress(clockInInfoVo.getAddress());
        LoginResultVO login = AutoSignUtils.login(clockInInfoVo);
        System.out.println(login);
        System.out.println(AutoSignUtils.sign(clockInInfoVo));
    }

    @Test
    void userRegister() {
        String userAccount = "qimu";
        String userPassword = "";
        String checkPassword = "123456";
        try {
            long result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu";
            result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "qimu";
            userPassword = "123456";
            result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "yu pi";
            userPassword = "12345678";
            result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            checkPassword = "123456789";
            result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "dogYupi";
            checkPassword = "12345678";
            result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
            userAccount = "qimu";
            result = userService.userRegister(userAccount, userPassword, checkPassword, checkPassword);
            Assertions.assertEquals(-1, result);
        } catch (Exception e) {

        }
    }
}