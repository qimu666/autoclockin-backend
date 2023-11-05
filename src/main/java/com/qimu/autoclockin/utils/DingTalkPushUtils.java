package com.qimu.autoclockin.utils;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.dingtalk.api.DefaultDingTalkClient;
import com.dingtalk.api.DingTalkClient;
import com.dingtalk.api.request.OapiRobotSendRequest;
import com.dingtalk.api.response.OapiRobotSendResponse;
import com.qimu.autoclockin.model.dto.dingTalk.DingTalkPushClient;
import com.taobao.api.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static com.qimu.autoclockin.constant.DingTalkPushConstant.*;

/**
 * @Author: QiMu
 * @Date: 2023年11月01日 11:50
 * @Version: 1.0
 * @Description:
 */
@Slf4j
public class DingTalkPushUtils {

    public static DingTalkClient initDingTalkClient(DingTalkPushClient dingTalkPushClient) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        Long timestamp = System.currentTimeMillis();
        String secret = dingTalkPushClient.getSecret();
        String stringToSign = timestamp + "\n" + secret;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), "UTF-8");
        return new DefaultDingTalkClient("https://oapi.dingtalk.com/robot/send?access_token=" + dingTalkPushClient.getAccessToken() + "&timestamp=" + timestamp + "&sign=" + sign);
    }

    /**
     * 通过文本发送消息
     *
     * @param content    所容纳之物
     * @param mobileList 手机列表
     * @param isAtAll    @全体
     * @return {@link OapiRobotSendResponse}
     */
    public static OapiRobotSendResponse sendMessageByText(DingTalkClient client, String content, List<String> mobileList,
                                                          boolean isAtAll) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }

        OapiRobotSendRequest.Text text = new OapiRobotSendRequest.Text();
        text.setContent(content);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        if (!CollectionUtils.isEmpty(mobileList)) {
            // 发送消息并@ 以下手机号联系人
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setAtMobiles(mobileList);
            at.setIsAtAll(isAtAll);
            request.setAt(at);
        }
        request.setMsgtype(MSG_TYPE_TEXT);
        request.setText(text);

        OapiRobotSendResponse response = new OapiRobotSendResponse();
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            log.error("[发送普通文本消息]: 发送消息失败, 异常捕获{}", e.getMessage());
        }
        return response;
    }

    /**
     * 通过链接发送消息
     *
     * @param title      标题
     * @param text       文本
     * @param messageUrl 消息url
     * @param picUrl     图片url
     * @return {@link OapiRobotSendResponse}
     */
    public static OapiRobotSendResponse sendMessageByLink(DingTalkClient client, String title, String text, String messageUrl, String
            picUrl) {
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(text) || StringUtils.isEmpty(messageUrl)) {
            return null;
        }
        // 参数	参数类型	必须	说明
        // msgtype	String	是	消息类型，此时固定为：link
        // title	String	是	消息标题
        // text	String	是	消息内容。如果太长只会部分展示
        // messageUrl	String	是	点击消息跳转的URL
        // picUrl	String	否	图片URL
        OapiRobotSendRequest.Link link = new OapiRobotSendRequest.Link();
        link.setTitle(title);
        link.setText(text);
        link.setMessageUrl(messageUrl);
        link.setPicUrl(picUrl);

        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype(MSG_TYPE_LINK);
        request.setLink(link);

        OapiRobotSendResponse response = new OapiRobotSendResponse();
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            log.error("[发送link 类型消息]: 发送消息失败, 异常捕获{}", e.getMessage());
        }
        return response;
    }


    /**
     * 发送Markdown格式消息
     *
     * @param title        标题
     * @param markdownText 支持markdown 编辑格式的文本信息
     * @param mobileList   消息@ 联系人
     * @param isAtAll      是否@ 全部
     * @param client       客户
     * @return {@link OapiRobotSendResponse}
     */
    public static OapiRobotSendResponse sendMessageByMarkdown(DingTalkClient client, String title, String
            markdownText, List<String> mobileList, boolean isAtAll) {
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(markdownText)) {
            return null;
        }
        // 参数	类型	必选	说明
        // msgtype	String	是	此消息类型为固定markdown
        // title	String	是	首屏会话透出的展示内容
        // text	String	是	markdown格式的消息
        // atMobiles	Array	否	被@人的手机号(在text内容里要有@手机号)
        // isAtAll	bool	否	@所有人时：true，否则为：false
        OapiRobotSendRequest.Markdown markdown = new OapiRobotSendRequest.Markdown();
        markdown.setTitle(title);
        markdown.setText(markdownText);

        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype(MSG_TYPE_MARKDOWN);
        request.setMarkdown(markdown);
        if (!CollectionUtils.isEmpty(mobileList)) {
            OapiRobotSendRequest.At at = new OapiRobotSendRequest.At();
            at.setIsAtAll(isAtAll);
            at.setAtMobiles(mobileList);
            request.setAt(at);
        }

        OapiRobotSendResponse response = new OapiRobotSendResponse();
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            log.error("[发送Markdown类型消息]: 发送消息失败, 异常捕获{}", e.getMessage());
        }
        return response;
    }

    /**
     * 通过行动卡发送消息
     *
     * @param title          消息标题, 会话消息会展示标题
     * @param markdownText   markdown格式的消息
     * @param singleTitle    单个按钮的标题
     * @param singleURL      单个按钮的跳转链接
     * @param btnOrientation 是否横向排列(true 横向排列, false 纵向排列)
     * @param hideAvatar     是否隐藏发消息者头像(true 隐藏头像, false 不隐藏)
     * @param client         客户
     * @return {@link OapiRobotSendResponse}
     */
    public static OapiRobotSendResponse sendMessageByActionCardSingle(DingTalkClient client, String title, String markdownText, String
            singleTitle, String singleURL, boolean btnOrientation, boolean hideAvatar) {
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(markdownText)) {
            return null;
        }
        // 参数	类型	必选	说明
        //    msgtype	string	true	此消息类型为固定actionCard
        //    title	string	true	首屏会话透出的展示内容
        //    text	string	true	markdown格式的消息
        //    singleTitle	string	true	单个按钮的方案。(设置此项和singleURL后btns无效)
        //    singleURL	string	true	点击singleTitle按钮触发的URL
        //    btnOrientation	string	false	0-按钮竖直排列，1-按钮横向排列
        //    hideAvatar	string	false	0-正常发消息者头像，1-隐藏发消息者头像
        OapiRobotSendRequest.Actioncard actionCard = new OapiRobotSendRequest.Actioncard();
        actionCard.setTitle(title);
        actionCard.setText(markdownText);
        actionCard.setSingleTitle(singleTitle);
        actionCard.setSingleURL(singleURL);
        // 此处默认为0
        actionCard.setBtnOrientation(btnOrientation ? "1" : "0");
        // 此处默认为0
        actionCard.setHideAvatar(hideAvatar ? "1" : "0");

        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype(MSG_TYPE_ACTION_CARD);
        request.setActionCard(actionCard);
        OapiRobotSendResponse response = new OapiRobotSendResponse();
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            log.error("[发送ActionCard 类型消息]: 整体跳转ActionCard类型的发送消息失败, 异常捕获{}", e.getMessage());
        }
        return response;
    }

    /**
     * 通过行动卡多功能发送消息
     *
     * @param title          标题
     * @param markdownText   文本
     * @param btns           按钮列表
     * @param btnOrientation 是否横向排列(true 横向排列, false 纵向排列)
     * @param hideAvatar     是否隐藏发消息者头像(true 隐藏头像, false 不隐藏)
     * @param client         客户
     * @return {@link OapiRobotSendResponse}
     */
    public static OapiRobotSendResponse sendMessageByActionCardMulti(DingTalkClient client, String title, String
            markdownText, List<OapiRobotSendRequest.Btns> btns, boolean btnOrientation, boolean hideAvatar) {
        if (StringUtils.isEmpty(title) || StringUtils.isEmpty(markdownText) || CollectionUtils.isEmpty(btns)) {
            return null;
        }
        // 参数	类型	必选	说明
        // msgtype	string	true	此消息类型为固定actionCard
        // title	string	true	首屏会话透出的展示内容
        // text	string	true	markdown格式的消息
        // btns	array	true	按钮的信息：title-按钮方案，actionURL-点击按钮触发的URL
        // btnOrientation	string	false	0-按钮竖直排列，1-按钮横向排列
        // hideAvatar	string	false	0-正常发消息者头像，1-隐藏发消息者头像
        OapiRobotSendRequest.Actioncard actionCard = new OapiRobotSendRequest.Actioncard();
        actionCard.setTitle(title);
        actionCard.setText(markdownText);
        // 此处默认为0
        actionCard.setBtnOrientation(btnOrientation ? "1" : "0");
        // 此处默认为0
        actionCard.setHideAvatar(hideAvatar ? "1" : "0");

        actionCard.setBtns(btns);

        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype(MSG_TYPE_ACTION_CARD);
        request.setActionCard(actionCard);
        OapiRobotSendResponse response = new OapiRobotSendResponse();
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            log.error("[发送ActionCard 类型消息]: 独立跳转ActionCard类型发送消息失败, 异常捕获{}", e.getMessage());
        }
        return response;
    }

    /**
     * 通过馈送卡发送消息
     *
     * @param client 客户
     * @param links  链接
     * @return {@link OapiRobotSendResponse}
     */
    public static OapiRobotSendResponse sendMessageByFeedCard(DingTalkClient client, List<OapiRobotSendRequest.Links> links) {
        if (CollectionUtils.isEmpty(links)) {
            return null;
        }

        // msgtype	string	true	此消息类型为固定feedCard
        // title	string	true	单条信息文本
        // messageURL	string	true	点击单条信息到跳转链接
        // picURL	string	true	单条信息后面图片的URL
        OapiRobotSendRequest.Feedcard feedcard = new OapiRobotSendRequest.Feedcard();
        feedcard.setLinks(links);
        OapiRobotSendRequest request = new OapiRobotSendRequest();
        request.setMsgtype(MSG_TYPE_FEED_CARD);
        request.setFeedCard(feedcard);
        OapiRobotSendResponse response = new OapiRobotSendResponse();
        try {
            response = client.execute(request);
        } catch (ApiException e) {
            log.error("[发送ActionCard 类型消息]: 独立跳转ActionCard类型发送消息失败, 异常捕获{}", e.getMessage());
        }
        return response;
    }

}
