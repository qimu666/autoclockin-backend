package com.qimu.autoclockin.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.qimu.autoclockin.common.ErrorCode;
import com.qimu.autoclockin.exception.BusinessException;
import com.qimu.autoclockin.model.dto.IpPool.IpPoolClient;
import com.qimu.autoclockin.model.dto.dingTalk.DingTalkPushClient;
import com.qimu.autoclockin.model.entity.User;
import com.qimu.autoclockin.utils.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Random;

import static com.qimu.autoclockin.constant.IpConstant.IP_URL;
import static com.qimu.autoclockin.utils.DingTalkPushUtils.initDingTalkClient;
import static com.qimu.autoclockin.utils.DingTalkPushUtils.sendMessageByMarkdown;

/**
 * 用户服务测试
 *
 * @author qimu
 */
@SpringBootTest
@Slf4j
class UserServiceTest {

    @Resource
    private UserService userService;

    @Resource
    private ClockInInfoService clockInInfoService;
    @Resource
    private IpPoolClient ipPoolClient;

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

    @Resource
    private DingTalkPushClient dingTalkPushClient;

    @Test
    public void sendMessageWebhook() throws Exception {
        String message = "<h1 align=\"center\">\n" +
                "   打卡成功\n" +
                "</h1><br/>\n" +
                "\n" +
                "- 打卡账号：**17744608948**\n" +
                "- 打卡状态：**打卡成功**\n" +
                "- 使用IP池：**是**\n" +
                "- 打卡状态描述：**打卡成功，待审核**\n" +
                "- 打卡时间：**" + DateUtil.now() + "**\n" +
                "- 打卡地址：**河南省许昌市市辖区芙蓉湖元鼎国际**\n";
        sendMessageByMarkdown(initDingTalkClient(dingTalkPushClient), "打卡成功", message, null, false);
    }

    @Test
    void userRegister() {

    }

    @Test
    void test() {
        String longitude = "114.351387";
        String latitude = "32.997650";

        System.out.println("随机经度：" + getRandomLatitudeAndLongitude(longitude));
        System.out.println("随机纬度：" + getRandomLatitudeAndLongitude(latitude));
    }

    private String getRandomLatitudeAndLongitude(String latitudeAndLongitude) {
        Random random = new Random();
        // 生成0到10之间的随机数
        int randomValue = random.nextInt(11);
        StringBuilder subLatitudeAndLongitude = new StringBuilder(latitudeAndLongitude);
        subLatitudeAndLongitude.replace(subLatitudeAndLongitude.length() - 1, subLatitudeAndLongitude.length(), String.valueOf(randomValue));
        return subLatitudeAndLongitude.toString();
    }

    @Test
    void getIp() {
        HttpResponse response = HttpRequest.get(buildUrl()).execute();
        if (response.getStatus() == 200) {
            String result = response.body();
            ResponseData responseData = JSONUtil.toBean(result, ResponseData.class);
            if (responseData.getCode() == -1 || responseData.getData() == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, responseData.getMessage());
            }
            List<ResponseData.IpInfo> list = responseData.getData().getList();
            ResponseData.IpInfo ipInfo = list.get(0);
            System.err.println(ipInfo);
            HttpResponse res = HttpRequest.get("https://sxbaapp.vae.ha.cn/")
                    .setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ipInfo.getIp(), Integer.parseInt(ipInfo.getPort()))))
                    .setConnectionTimeout(4000)
                    .execute();
            if (res.getStatus() != 200) {
            }
            System.err.println(res.body());
        } else {
            System.err.println("ip获取失败");
        }
    }

    private String buildUrl() {
        String url = IP_URL
                + "?repeat=1"
                + "&format=" + ipPoolClient.getFormat()
                + "&protocol=" + ipPoolClient.getProtocol()
                + "&num=" + ipPoolClient.getExtractQuantity()
                + "&no=" + ipPoolClient.getPackageNumber()
                + "&mode=" + ipPoolClient.getAuthorizationMode()
                + "&secret=" + ipPoolClient.getPackageSecret()
                + "&minute=" + ipPoolClient.getOccupancyDuration()
                + "&pool=" + ipPoolClient.getIpPoolType();
        log.info("获取ip的url为：" + url);
        return url;
    }
}