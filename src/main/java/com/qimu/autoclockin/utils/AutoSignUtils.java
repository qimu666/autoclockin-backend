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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.qimu.autoclockin.constant.IpConstant.IP_POOL_KEY;
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
        HEADERS.put("cl_ip", "192.168.190.1");
    }

    /**
     * 获取token
     *
     * @return {@link String}
     */
    private static String getToken() {
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
            String result = HttpRequest.post(LOGIN_URL).addHeaders(HEADERS).body(JSONUtil.toJsonStr(loginData)).execute().body();
            log.info("login result :{}", result);
            LoginResultVO loginResultVO = new LoginResultVO();
            loginResultVO.setLoginResult(JSONUtil.toBean(result, LoginResult.class));
            loginResultVO.setToken(tokenResult.getData().getToken());
            return loginResultVO;
        } else if (Objects.nonNull(tokenResult) && tokenResult.getCode() != SUCCESS_CODE) {
            log.error("login {}", tokenResult.getMsg());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, tokenResult.getMsg());
        } else {
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
    public static ClockInStatus sign(boolean enable, ClockInInfoVo clockInInfoVo, String ipPoolUrl, RedisTemplate<String, String> redisTemplate) throws Exception {
        ResponseData.IpInfo ipInfo = checkCacheIpInfo(enable, clockInInfoVo, ipPoolUrl, redisTemplate);
        // 登录
        LoginResultVO loginResultVO = login(clockInInfoVo);
        LoginResult loginResult = loginResultVO.getLoginResult();
        TimeUnit.SECONDS.sleep(1);
        ClockInStatus clockInStatus = new ClockInStatus();
        if (ObjectUtils.isNotEmpty(loginResultVO) && loginResult.getCode() == SUCCESS_CODE) {
            return signRequest(ipInfo, clockInInfoVo, loginResultVO, loginResult);
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

    /**
     * 检查缓存ip信息
     *
     * @param enable        使可能
     * @param clockInInfoVo 登录信息vo
     * @param ipPoolUrl     ip池url
     * @param redisTemplate redis模板
     * @return {@link ResponseData.IpInfo}
     */
    private static ResponseData.IpInfo checkCacheIpInfo(boolean enable, ClockInInfoVo clockInInfoVo, String ipPoolUrl, RedisTemplate<String, String> redisTemplate) {
        ResponseData.IpInfo ipInfo = null;
        if (enable) {
            String cacheIpInfo = redisTemplate.opsForValue().get(IP_POOL_KEY + clockInInfoVo.getUserId());
            if (StringUtils.isNotBlank(cacheIpInfo)) {
                ipInfo = JSONUtil.toBean(cacheIpInfo, ResponseData.IpInfo.class);
            } else {
                ResponseData.IpInfo getIpInfo = getIpInfo(clockInInfoVo.getUserId(), ipPoolUrl, redisTemplate);
                if (getIpInfo == null) {
                    redisTemplate.delete(IP_POOL_KEY + clockInInfoVo.getUserId());
                }
                ipInfo = getIpInfo;
            }
        }
        return ipInfo;
    }

    /**
     * 获取ip信息
     *
     * @param userId        用户id
     * @param ipPoolUrl     ip池url
     * @param redisTemplate redis模板
     * @return {@link ResponseData.IpInfo}
     */
    private static ResponseData.IpInfo getIpInfo(Long userId, String ipPoolUrl, RedisTemplate<String, String> redisTemplate) {
        cn.hutool.http.HttpResponse response = HttpRequest.get(ipPoolUrl).execute();
        if (response.getStatus() == 200) {
            String result = response.body();
            if (StringUtils.isBlank(result)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "ip池获取ip失败,已重试");
            }
            ResponseData responseData = JSONUtil.toBean(JSONUtil.toJsonStr(result), ResponseData.class);
            if (responseData.getCode() == -1 || responseData.getData() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, responseData.getMessage());
            }
            List<ResponseData.IpInfo> list = responseData.getData().getList();
            ResponseData.IpInfo ipInfo = list.get(0);
            boolean checkProxyIp = checkProxyIp(userId, ipInfo, redisTemplate);
            if (checkProxyIp) {
                return ipInfo;
            }
        }
        return null;
    }

    /**
     * 检查代理ip
     *
     * @param userId        用户id
     * @param ipInfo        ip信息
     * @param redisTemplate redis模板
     * @return boolean
     */
    private static boolean checkProxyIp(Long userId, ResponseData.IpInfo ipInfo, RedisTemplate<String, String> redisTemplate) {
        cn.hutool.http.HttpResponse res = HttpRequest.get("https://sxbaapp.vae.ha.cn/").setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipInfo.getIp(), Integer.parseInt(ipInfo.getPort())))).setConnectionTimeout(4000).execute();
        if (res.getStatus() == 200) {
            log.info("ip检测成功，信息为：" + JSONUtil.toJsonStr(ipInfo));
            redisTemplate.opsForValue().set(IP_POOL_KEY + userId, JSONUtil.toJsonStr(ipInfo), 3, TimeUnit.MINUTES);
            return true;
        } else {
            redisTemplate.delete(IP_POOL_KEY + userId);
            return false;
        }
    }

    /**
     * 打卡请求
     *
     * @param ipInfo        ip信息
     * @param clockInInfoVo 登录信息vo
     * @param loginResultVO 登录结果vo
     * @param loginResult   登录结果
     * @return {@link ClockInStatus}
     * @throws InterruptedException 中断异常
     */
    private static ClockInStatus signRequest(ResponseData.IpInfo ipInfo, ClockInInfoVo clockInInfoVo, LoginResultVO loginResultVO, LoginResult loginResult) throws InterruptedException {
        SignData signData = new SignData();
        signData.setDtype(1);
        signData.setProbability(2);
        signData.setAddress(clockInInfoVo.getAddress());
        signData.setLongitude(getRandomLatitudeAndLongitude(clockInInfoVo.getLongitude()));
        signData.setLatitude(getRandomLatitudeAndLongitude(clockInInfoVo.getLatitude()));
        signData.setPhonetype(clockInInfoVo.getDeviceType());
        signData.setUid(loginResult.getData().getUid());
        // 消息认证码算法
        String sign = hashMessageAuthenticationCode(signData, loginResultVO.getToken());
        HEADERS.put("sign", sign);
        HEADERS.put("phone", clockInInfoVo.getDeviceType());
        TimeUnit.SECONDS.sleep(1);
        HttpRequest httpRequest = HttpRequest.post(SIGN_URL).addHeaders(HEADERS).body(JSONUtil.toJsonStr(signData));
        if (ipInfo != null) {
            log.info("请求使用代理ip池：{}:{}", ipInfo.getIp(), ipInfo.getPort());
            httpRequest.setHttpProxy(ipInfo.getIp(), Integer.parseInt(ipInfo.getPort()));
        }
        cn.hutool.http.HttpResponse response = httpRequest.execute();
        ClockInStatus clockInStatus = new ClockInStatus();
        if (response.getStatus() != 200) {
            clockInStatus.setStatus(false);
            clockInStatus.setMessage("打卡失败，将在15分钟后重试");
            return clockInStatus;
        }
        String result = response.body();

        log.info("sign result : 【{}】", result);
        if (StringUtils.isBlank(result)) {
            clockInStatus.setStatus(false);
            clockInStatus.setMessage("sign 打卡异常，将在15分钟后重试");
            return clockInStatus;
        }
        LoginResult loginResponse = JSONUtil.toBean(JSONUtil.toJsonStr(result), LoginResult.class);
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
            clockInStatus.setMessage("sign 打卡异常，将在15分钟后重试");
            return clockInStatus;
        }
    }

    /**
     * 随机经纬度
     *
     * @param latitudeAndLongitude 经纬度
     * @return 新的经纬度
     */
    private static String getRandomLatitudeAndLongitude(String latitudeAndLongitude) {
        Random random = new Random();
        // 生成0到10之间的随机数
        int randomValue = random.nextInt(11);
        StringBuilder subLatitudeAndLongitude = new StringBuilder(latitudeAndLongitude);
        // 截取最后以为，替换为0-10之间的随机数，组装为新的字符串
        subLatitudeAndLongitude.replace(subLatitudeAndLongitude.length() - 1, subLatitudeAndLongitude.length(), String.valueOf(randomValue));
        return subLatitudeAndLongitude.toString();
    }
}