package com.qimu.autoclockin.utils;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

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
        HEADERS.put("appversion", "56");
        HEADERS.put("content-type", "application/json;charset=UTF-8");
        HEADERS.put("accept-encoding", "gzip");
        HEADERS.put("user-agent", "okhttp/3.14.9");
    }

    /**
     * 获取token
     *
     * @return {@link String}
     */
    private static String getToken() throws Exception {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(TOKEN_URL);
            httpPost.setHeader("content-type", "application/json;charset=UTF-8");
            StringEntity requestEntity = new StringEntity("", ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseBody = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
            return responseBody;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
    }

    /**
     * 消息认证码算法
     *
     * @param additionalText 附加文本
     * @param signData       加密数据
     * @return {@link String}
     */
    private static <T> String hashMessageAuthenticationCode(T signData, String additionalText) {
        // json格式登录数据
        String jsonData = JSONUtil.toJsonStr(signData);
        String combinedText = jsonData + additionalText;
        // 计算 HmacSHA256
        return new HMac(HmacAlgorithm.HmacSHA256, SECRET_KEY.getBytes()).digestHex(combinedText);
    }

    /**
     * 登录
     *
     * @param clockInInfoVo 打卡信息签证官
     * @return {@link LoginResultVO}
     * @throws InterruptedException 中断异常
     */
    public static LoginResultVO login(ClockInInfoVo clockInInfoVo) throws Exception {
        // 获取token
        TokenResponse tokenResult = JSONUtil.toBean(JSONUtil.toJsonStr(getToken()), TokenResponse.class);
        if (Objects.nonNull(tokenResult) && tokenResult.getCode() == SUCCESS_CODE) {
            LoginData loginData = new LoginData();
            loginData.setPassword(MD5.create().digestHex(clockInInfoVo.getClockPassword()));
            loginData.setPhone(clockInInfoVo.getClockInAccount());
            loginData.setDtype(DTYPE);
            loginData.setDToken(clockInInfoVo.getDeviceId());
            // 消息认证码算法
            String sign = hashMessageAuthenticationCode(loginData, tokenResult.getData().getToken());
            HEADERS.put("sign", sign);
            HEADERS.put("phone", clockInInfoVo.getClockInAccount());
            TimeUnit.SECONDS.sleep(1);
            String result = HttpRequest.post(LOGIN_URL)
                    .addHeaders(HEADERS)
                    .body(JSONUtil.toJsonStr(loginData))
                    .execute().body();
            log.info("login result :{}", result);
            LoginResultVO loginResultVO = new LoginResultVO();
            loginResultVO.setLoginResult(JSONUtil.toBean(result, LoginResult.class));
            loginResultVO.setToken(tokenResult.getData().getToken());
            return loginResultVO;
        } else if (Objects.nonNull(tokenResult) && tokenResult.getCode() != SUCCESS_CODE) {
            log.error("login {}", tokenResult.getMsg());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, tokenResult.getMsg());
        } else {
            log.error("login token获取异常！");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "token获取异常");
        }
    }

    /**
     * 打卡
     *
     * @param clockInInfoVo 打卡信息签证
     * @return boolean
     * @throws InterruptedException 中断异常
     */
    public static ClockInStatus sign(ClockInInfoVo clockInInfoVo) throws Exception {
        // 登录
        LoginResultVO loginResultVO = login(clockInInfoVo);
        LoginResult loginResult = loginResultVO.getLoginResult();
        TimeUnit.SECONDS.sleep(1);
        ClockInStatus clockInStatus = new ClockInStatus();
        if (ObjectUtils.isNotEmpty(loginResultVO) && loginResult.getCode() == SUCCESS_CODE) {
            return signRequest(clockInInfoVo, loginResultVO, loginResult);
        } else if (ObjectUtils.isNotEmpty(loginResult) && loginResult.getCode() != SUCCESS_CODE) {
            log.error("sign {} ", loginResult.getMsg());
            clockInStatus.setStatus(false);
            clockInStatus.setMessage(loginResult.getMsg());
            return clockInStatus;
        } else {
            clockInStatus.setStatus(false);
            clockInStatus.setMessage("sign 打卡异常");
            return clockInStatus;
        }
    }

    private static ClockInStatus signRequest(ClockInInfoVo clockInInfoVo, LoginResultVO loginResultVO, LoginResult loginResult) throws InterruptedException {
        SignData signData = new SignData();
        signData.setDtype(1);
        signData.setProbability(2);
        signData.setAddress(clockInInfoVo.getAddress());
        signData.setLongitude(clockInInfoVo.getLongitude());
        signData.setLatitude(clockInInfoVo.getLatitude());
        signData.setPhonetype(clockInInfoVo.getDeviceType());
        signData.setUid(loginResult.getData().getUid());
        // 消息认证码算法
        String sign = hashMessageAuthenticationCode(signData, loginResultVO.getToken());
        HEADERS.put("sign", sign);
        HEADERS.put("phone", clockInInfoVo.getDeviceType());
        TimeUnit.SECONDS.sleep(2);
        String result = HttpRequest.post(SIGN_URL).addHeaders(HEADERS).body(JSONUtil.toJsonStr(signData)).execute().body();
        ClockInStatus clockInStatus = new ClockInStatus();
        log.info("sign result : {}", result);
        LoginResult loginResponse = JSONUtil.toBean(result, LoginResult.class);
        if (Objects.nonNull(loginResponse) && loginResponse.getCode() == SUCCESS_CODE) {
            clockInStatus.setStatus(true);
            clockInStatus.setMessage(loginResponse.getMsg());
            return clockInStatus;
        } else if (Objects.nonNull(loginResponse) && loginResponse.getCode() != SUCCESS_CODE) {
            log.error("sign {}", loginResponse.getMsg());
            clockInStatus.setStatus(false);
            clockInStatus.setMessage(loginResponse.getMsg());
            return clockInStatus;
        } else {
            clockInStatus.setStatus(false);
            clockInStatus.setMessage("sign 打卡异常");
            return clockInStatus;
        }
    }
}