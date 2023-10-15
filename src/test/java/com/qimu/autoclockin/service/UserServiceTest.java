package com.qimu.autoclockin.service;

import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.model.vo.ClockInInfoVo;
import com.qimu.autoclockin.model.vo.LoginResult;
import com.qimu.autoclockin.model.vo.LoginResultVO;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static com.qimu.autoclockin.utils.AutoSignUtils.login;

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

    }

    @Test
    void userRegister() {
        ClockInInfoVo clockInInfoVo = new ClockInInfoVo();
        clockInInfoVo.setClockInAccount("17748");
        clockInInfoVo.setClockPassword("bf334");
        clockInInfoVo.setDeviceId("a96069e250e38b1b62984545333af272ce325b03");
        try {
            LoginResultVO loginResultVO = login(clockInInfoVo);
            LoginResult loginResult = loginResultVO.getLoginResult();
            if (ObjectUtils.anyNull(loginResult, loginResultVO)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号测试失败：" + loginResult.getMsg());
            }
            if (ObjectUtils.isNotEmpty(loginResult) && loginResult.getCode() != 1001) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "账号测试失败：" + loginResult.getMsg());
            }
        } catch (InterruptedException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, e.getMessage());
        }
    }
}