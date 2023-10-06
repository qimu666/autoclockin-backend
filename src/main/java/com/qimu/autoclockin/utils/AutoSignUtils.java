package com.qimu.autoclockin.utils;

import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.qimu.autoclockin.constant.RequestAddressConstant.*;

/**
 * @author qimu
 */
@Slf4j
public class AutoSignUtils {

    /**
     * 请求头
     */
    private static final Map<String, String> HEADERS = new HashMap<>();

    static {
        HEADERS.put("os", "android");
        HEADERS.put("appVersion", "51");
        HEADERS.put("Sign", "Sign");
        HEADERS.put("cl_ip", "192.168.1.3");
        HEADERS.put("User-Agent", "okhttp/3.14.9");
        HEADERS.put("Content-Type", "application/json;charset=utf-8");
    }

    /**
     * 获取token
     *
     * @param clockInInfoVo 打卡信息签证官
     * @return {@link String}
     */
    private static String getToken(ClockInInfoVo clockInInfoVo) {
        // 添加手机型号
        HEADERS.put("phone", clockInInfoVo.getDeviceType());
        String body = HttpRequest.post(TOKEN_URL)
                .addHeaders(HEADERS)
                .execute().body();
        log.info("=== AutoSignUtils getToken body :{} ===", body);
        return body;
    }

    /**
     * 登录
     *
     * @param clockInInfoVo 打卡信息签证官
     * @return {@link LoginResultVO}
     * @throws InterruptedException 中断异常
     */
    public static LoginResultVO login(ClockInInfoVo clockInInfoVo) throws InterruptedException {
        // 获取token
        TokenResponse tokenResult = JSONUtil.toBean(getToken(clockInInfoVo), TokenResponse.class);
        if (Objects.nonNull(tokenResult) && tokenResult.getCode() == 1001) {
            LoginData loginData = new LoginData();
            loginData.setPassword(clockInInfoVo.getUserPassword());
            loginData.setPhone(clockInInfoVo.getUserAccount());
            loginData.setDtype(6);
            loginData.setDToken(clockInInfoVo.getDeviceId());
            HEADERS.put("Sign", MD5.create().digestHex(JSONUtil.toJsonStr(loginData) + tokenResult.getData().getToken()));
            TimeUnit.SECONDS.sleep(1);
            String result = HttpRequest.post(LOGIN_URL).addHeaders(HEADERS).body(JSONUtil.toJsonStr(loginData)).execute().body();
            log.info("login result :{}", result);
            LoginResultVO loginResultVO = new LoginResultVO();
            loginResultVO.setLoginResult(JSONUtil.toBean(result, LoginResult.class));
            loginResultVO.setToken(tokenResult.getData().getToken());
            return loginResultVO;
        } else if (Objects.nonNull(tokenResult) && tokenResult.getCode() != 1001) {
            log.error("login {}", tokenResult.getMsg());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, tokenResult.getMsg());
        } else {
            log.error("login token获取异常！");
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"token获取异常");
        }
    }

    /**
     * 打卡
     *
     * @param clockInInfoVo 打卡信息签证
     * @return boolean
     * @throws InterruptedException 中断异常
     */
    public static boolean sign(ClockInInfoVo clockInInfoVo) throws InterruptedException {
        // 登录
        LoginResultVO loginResultVO = login(clockInInfoVo);
        LoginResult loginResult = loginResultVO.getLoginResult();

        if (ObjectUtils.isNotEmpty(loginResultVO) && loginResult.getCode() == 1001) {
            SignData signData = new SignData();
            signData.setDtype(1);
            signData.setProbability(-1);
            signData.setAddress(clockInInfoVo.getAddress());
            signData.setLongitude(clockInInfoVo.getLongitude());
            signData.setLatitude(clockInInfoVo.getLatitude());
            signData.setPhonetype(clockInInfoVo.getDeviceType());
            signData.setUid(loginResult.getData().getUid());
            HEADERS.put("Sign", MD5.create().digestHex(JSONUtil.toJsonStr(signData) + loginResultVO.getToken()));
            TimeUnit.SECONDS.sleep(2);
            String result = HttpRequest.post(SIGN_URL).addHeaders(HEADERS).body(JSONUtil.toJsonStr(signData)).execute().body();
            log.info("sign result : {}", result);
            LoginResult loginResponse = JSONUtil.toBean(result, LoginResult.class);
            if (Objects.nonNull(loginResponse) && loginResponse.getCode() == 1001) {
                return true;
            } else if (Objects.nonNull(loginResponse) && loginResponse.getCode() != 1001) {
                log.error("sign {}", loginResponse.getMsg());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, loginResponse.getMsg());
            } else {
                log.error("sign 打卡异常");
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"打卡异常");
            }
        } else if (ObjectUtils.isNotEmpty(loginResult) && loginResult.getCode() != 1001) {
            log.error("sign {} ", loginResult.getMsg());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, loginResult.getMsg());
        } else {
            log.error("sign 登录异常");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "sign 登录异常");
        }
    }
}