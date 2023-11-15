package com.qimu.autoclockin.utils;

import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.autoclockin.AutoClockInClient;
import com.qimu.autoclockin.model.dto.tencentmap.TencentMapClient;
import com.qimu.autoclockin.model.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.qimu.autoclockin.constant.IpConstant.IP_POOL_KEY;
import static com.qimu.autoclockin.constant.RequestAddressConstant.*;
import static com.qimu.autoclockin.model.enums.IpPoolStatusEnum.STARTING;

/**
 * @author qimu
 */
@Slf4j
public class AutoSignUtils {


    /**
     * 获取token
     *
     * @param autoClockInClient 自动登录客户端
     * @return {@link String}
     */
    private static String getToken(AutoClockInClient autoClockInClient) {
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpPost httpPost = new HttpPost(autoClockInClient.getTokenUrl());
            httpPost.setHeader("content-type", "application/json;charset=UTF-8");
            StringEntity requestEntity = new StringEntity("", ContentType.APPLICATION_JSON);
            RequestConfig requestConfig = RequestConfig.custom()
                    // 设置连接超时时间为5秒
                    .setConnectTimeout(5000)
                    // 设置读取超时时间为5秒
                    .setSocketTimeout(5000)
                    .build();
            httpPost.setConfig(requestConfig);
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
     * @param clockInInfoVo     打卡信息签证官
     * @param autoClockInClient 自动登录客户端
     * @return {@link LoginResultVO}
     * @throws Exception 例外
     */
    public static LoginResultVO login(ClockInInfoVo clockInInfoVo, AutoClockInClient autoClockInClient) throws Exception {
        // 获取token
        TokenResponse tokenResult = JSONUtil.toBean(JSONUtil.toJsonStr(getToken(autoClockInClient)), TokenResponse.class);
        if (Objects.nonNull(tokenResult) && tokenResult.getCode() == SUCCESS_CODE) {
            LoginData loginData = new LoginData();
            loginData.setPassword(MD5.create().digestHex(clockInInfoVo.getClockPassword()));
            loginData.setPhone(clockInInfoVo.getClockInAccount());
            loginData.setDtype(DTYPE);
            loginData.setDToken(clockInInfoVo.getDeviceId());
            // 消息认证码算法
            String sign = hashMessageAuthenticationCode(loginData, tokenResult.getData().getApitoken());
            TimeUnit.SECONDS.sleep(1);

            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                HttpPost httpPost = new HttpPost(autoClockInClient.getLoginUrl());
                httpPost.addHeader("os", autoClockInClient.getOs());
                httpPost.addHeader("appversion", autoClockInClient.getAppVersion());
                httpPost.addHeader("content-type", CONTENT_TYPE);
                httpPost.addHeader("accept-encoding", ACCEPT_ENCODING);
                httpPost.addHeader("user-agent", USER_AGENT);
                httpPost.addHeader("cl_ip", CL_IP);
                httpPost.addHeader("sign", sign);
                httpPost.addHeader("phone", clockInInfoVo.getClockInAccount());
                httpPost.addHeader("token", tokenResult.getData().getApitoken());
                httpPost.addHeader("timestamp", String.valueOf(System.currentTimeMillis()));
                log.info("login request data :{}", loginData);
                StringEntity requestEntity = new StringEntity(JSONUtil.toJsonStr(loginData), ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);
                RequestConfig requestConfig = RequestConfig.custom()
                        // 设置连接超时时间为5秒
                        .setConnectTimeout(5000)
                        // 设置读取超时时间为5秒
                        .setSocketTimeout(5000)
                        .build();
                httpPost.setConfig(requestConfig);
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity responseEntity = response.getEntity();
                String result = EntityUtils.toString(responseEntity);
                EntityUtils.consume(responseEntity);
                log.info("login result :{}", result);
                LoginResultVO loginResultVO = new LoginResultVO();
                loginResultVO.setLoginResult(JSONUtil.toBean(result, LoginResult.class));
                loginResultVO.setToken(loginResultVO.getLoginResult().getData().getUserToken());
                return loginResultVO;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR);
            }
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
     * @param clockInInfoVo     打卡信息签证
     * @param enable            使可能
     * @param ipPoolUrl         ip池url
     * @param redisTemplate     redis模板
     * @param tencentMapClient  腾讯地图客户端
     * @param autoClockInClient 自动登录客户端
     * @return boolean
     * @throws Exception 异常
     */
    public static ClockInStatus sign(boolean enable, ClockInInfoVo clockInInfoVo, String ipPoolUrl, RedisTemplate<String, String> redisTemplate, TencentMapClient tencentMapClient, AutoClockInClient autoClockInClient) throws Exception {
        clockInInfoVo = doTrimParamsClockInInfoVo(clockInInfoVo);

        ResponseData.IpInfo ipInfo = checkCacheIpInfo(enable && clockInInfoVo.getIsEnable().equals(STARTING.getValue()), clockInInfoVo, ipPoolUrl, redisTemplate);
        // 登录
        LoginResultVO loginResultVO = login(clockInInfoVo,autoClockInClient);
        LoginResult loginResult = loginResultVO.getLoginResult();
        TimeUnit.SECONDS.sleep(1);
        ClockInStatus clockInStatus = new ClockInStatus();
        if (ObjectUtils.isNotEmpty(loginResultVO) && loginResult.getCode() == SUCCESS_CODE) {
            return signRequest(ipInfo, clockInInfoVo, loginResultVO, loginResult, tencentMapClient,autoClockInClient);
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
     * 去除参数中的空格
     *
     * @param clockInInfoVo 登录信息vo
     * @return {@link ClockInInfoVo}
     */
    private static ClockInInfoVo doTrimParamsClockInInfoVo(ClockInInfoVo clockInInfoVo) {
        ClockInInfoVo clock = new ClockInInfoVo();
        BeanUtils.copyProperties(clockInInfoVo, clock);
        clock.setAddress(clockInInfoVo.getAddress().trim());
        clock.setClockInAccount(clockInInfoVo.getClockInAccount().trim());
        clock.setClockPassword(clock.getClockPassword().trim());
        clock.setDeviceType(clock.getDeviceType().trim());
        clock.setDeviceId(clock.getDeviceId().trim());
        clock.setLongitude(clock.getLongitude().trim());
        clock.setLatitude(clock.getLatitude().trim());
        return clock;
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
        cn.hutool.http.HttpResponse response = HttpRequest.get(ipPoolUrl).setReadTimeout(5000).setConnectionTimeout(5000).execute();
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
     * @param ipInfo            ip信息
     * @param clockInInfoVo     登录信息vo
     * @param loginResultVO     登录结果vo
     * @param loginResult       登录结果
     * @param autoClockInClient
     * @return {@link ClockInStatus}
     * @throws InterruptedException 中断异常
     */
    private static ClockInStatus signRequest(ResponseData.IpInfo ipInfo, ClockInInfoVo clockInInfoVo, LoginResultVO loginResultVO, LoginResult loginResult, TencentMapClient tencentMapClient, AutoClockInClient autoClockInClient) throws InterruptedException {
        SignData signData = new SignData();
        signData.setDtype(1);
        signData.setProbability(0);
        // 使用腾讯地图获取地址，同职校家园一样
        if (tencentMapClient.isEnable()) {
            String address = getAddress(getRandomLatitudeAndLongitude(clockInInfoVo.getLatitude()), getRandomLatitudeAndLongitude(clockInInfoVo.getLongitude()), tencentMapClient.getKey());
            signData.setAddress(address);
        } else {
            signData.setAddress(clockInInfoVo.getAddress());
        }
        signData.setLongitude(getRandomLatitudeAndLongitude(clockInInfoVo.getLongitude()));
        signData.setLatitude(getRandomLatitudeAndLongitude(clockInInfoVo.getLatitude()));
        signData.setPhonetype(clockInInfoVo.getDeviceType());
        signData.setUid(loginResult.getData().getUid());
        // 消息认证码算法
        String sign = hashMessageAuthenticationCode(signData, loginResultVO.getToken());
        TimeUnit.SECONDS.sleep(1);
        ClockInStatus clockInStatus = new ClockInStatus();
        try {
            CloseableHttpClient client = null;
            HttpPost httpPost = new HttpPost(autoClockInClient.getSignUrl());
            httpPost.addHeader("os", autoClockInClient.getOs());
            httpPost.addHeader("appversion", autoClockInClient.getAppVersion());
            httpPost.addHeader("content-type", CONTENT_TYPE);
            httpPost.addHeader("accept-encoding", ACCEPT_ENCODING);
            httpPost.addHeader("user-agent", USER_AGENT);
            httpPost.addHeader("cl_ip", CL_IP);
            httpPost.addHeader("sign", sign);
            httpPost.addHeader("phone", clockInInfoVo.getDeviceType());
            httpPost.addHeader("token", loginResultVO.getToken());
            httpPost.addHeader("timestamp", String.valueOf(System.currentTimeMillis()));
            log.info("sign request data :{}", signData);
            StringEntity requestEntity = new StringEntity(JSONUtil.toJsonStr(signData), ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);

            if (ipInfo != null) {
                log.info("请求使用代理ip池：{}:{}", ipInfo.getIp(), ipInfo.getPort());
                HttpHost proxy = new HttpHost(ipInfo.getIp(), Integer.parseInt(ipInfo.getPort()), "HTTP");
                client = HttpClients.custom()
                        .setProxy(proxy)
                        .build();
            } else {
                client = HttpClientBuilder.create().build();
            }
            RequestConfig requestConfig = RequestConfig.custom()
                    // 设置连接超时时间为5秒
                    .setConnectTimeout(5000)
                    // 设置读取超时时间为5秒
                    .setSocketTimeout(5000)
                    .build();
            httpPost.setConfig(requestConfig);
            HttpResponse response = client.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String result = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
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
                if (loginResponse.getMsg().contains("失败")) {
                    PallBottomUserInfo pallBottomUserInfo = new PallBottomUserInfo();
                    pallBottomUserInfo.setPhone(clockInInfoVo.getClockInAccount());
                    pallBottomUserInfo.setPassword(clockInInfoVo.getClockPassword());
                    pallBottomUserInfo.setDeviceType(clockInInfoVo.getDeviceType());
                    pallBottomUserInfo.setDeviceId(clockInInfoVo.getDeviceId());
                    pallBottomUserInfo.setAddress(clockInInfoVo.getAddress());
                    pallBottomUserInfo.setLongitude(clockInInfoVo.getLongitude());
                    pallBottomUserInfo.setLatitude(clockInInfoVo.getLatitude());
                    return pallBottomClockIn(pallBottomUserInfo);
                }
                clockInStatus.setStatus(false);
                clockInStatus.setMessage(loginResponse.getMsg());
                return clockInStatus;
            } else {
                clockInStatus.setStatus(false);
                clockInStatus.setMessage("sign 打卡异常，将在15分钟后重试");
                return clockInStatus;
            }
        } catch (IOException e) {
            clockInStatus.setStatus(false);
            clockInStatus.setMessage(StringUtils.isNotBlank(e.getMessage()) ? e.getMessage() : "sign 打卡异常，将在15分钟后重试");
            return clockInStatus;
        }
    }

    /**
     * pall底部时钟
     *
     * @param pallBottomUserInfo pall底部用户信息
     * @return {@link ClockInStatus}
     */
    private static ClockInStatus pallBottomClockIn(PallBottomUserInfo pallBottomUserInfo) {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        stringStringHashMap.put("Auth", "1111");
        stringStringHashMap.put("content-type", "application/json;charset=UTF-8");
        String body = HttpRequest.post("http://dk.sxba.api.xuanran.cc/")
                .addHeaders(stringStringHashMap)
                .body(JSONUtil.toJsonStr(pallBottomUserInfo))
                .timeout(5000)
                .execute()
                .body();
        PallBottomResponse pallBottomResponse = JSONUtil.toBean(body, PallBottomResponse.class);
        log.info("兜底打卡状态：" + pallBottomResponse);
        ClockInStatus clockInStatus = new ClockInStatus();
        if ("true".equals(pallBottomResponse.getSuccess())) {
            clockInStatus.setStatus(true);
            clockInStatus.setMessage(pallBottomResponse.getMessage());
            return clockInStatus;
        }
        clockInStatus.setStatus(false);
        clockInStatus.setMessage(pallBottomResponse.getMessage());
        return clockInStatus;
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

    /**
     * 获取地址
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @param mapKey    腾讯地图请求key
     * @return 新的经纬度
     */
    private static String getAddress(String latitude, String longitude, String mapKey) {
        if (StringUtils.isAnyBlank(latitude, latitude)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "经纬度不能为空");
        }
        String latitudeAndLongitude = latitude.trim() + "," + longitude.trim();
        String result = HttpRequest.get("https://apis.map.qq.com/ws/geocoder/v1/?location="
                + latitudeAndLongitude + "&key=" + mapKey + "&get_poi=1").execute().body();
        MapResult mapResult = JSONUtil.toBean(result, MapResult.class);
        if ("0".equals(mapResult.getStatus())) {
            log.info("打卡地址为：" + mapResult);
            return mapResult.getResult().getAddress();
        } else {
            log.info("地址获取失败：" + mapResult.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, mapResult.getMessage());
        }
    }
}