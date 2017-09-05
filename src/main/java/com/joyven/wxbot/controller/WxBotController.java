package com.joyven.wxbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.WriterException;
import com.joyven.wxbot.pojo.WxError;
import com.joyven.wxbot.pojo.WxUuid;
import com.joyven.wxbot.service.WxBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Date: 2017/9/4
 * Time: 下午6:06
 * Description:
 *
 * @author zhoujunwen
 * @version 1.0
 */
@RestController
@RequestMapping("wxbot")
public class WxBotController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WxBotController.class);
    // 登录后的跳转地址
    private String redirectUrl;
    @Resource
    private WxBotService wxBotService;

    @RequestMapping(value = "index", method = RequestMethod.GET)
    public Object index() throws IOException, InterruptedException {
        // 获取UUID
        LOGGER.info("开始登录微信客户端：{}", System.currentTimeMillis());
        WxUuid wxUuid = wxBotService.getUuid();

        if (wxUuid == null || wxUuid.getUuid() == null || "".equals(wxUuid.getUuid().trim())) {
            LOGGER.error("获取微信uuid失败");
        }

        // 生成二维码
        String image = null;
        try {
            image = wxBotService.genQrCode(wxUuid.getUuid());
        } catch (WriterException e) {
            e.printStackTrace();
        }

        checkLogin(wxUuid.getUuid());

        login();

        return image;
    }

    private WxError login() {
        String url = redirectUrl;
        if (url == null) {
            throw new RuntimeException("跳转失败！");
        }
       return wxBotService.login(url);
    }

    private int checkLogin(String uuid) throws InterruptedException {
        Thread.sleep(10000L);
        String res = checkResult(uuid, 3);

        if (res.contains("201")) {
            // 扫描成功，未确认登录
            LOGGER.info("扫描成功...");
            res = checkResult(uuid, 1);
            return 1;
        }
        if (res.contains("408")) {
            // 登陆超时
            LOGGER.info("登录超时...");
            return -1;
        }

        if (res.contains("400")) {
            LOGGER.info("登录异常...");
            return -2;

        }
        if (res.contains("200")) {
            // 确认登录
            LOGGER.info("登录成功!");
            int start = res.indexOf("https");
            res = res.substring(start).replace("\";", "");
            redirectUrl = res;
            return 2;
        }
        return 0;

    }


    private String checkResult(String uuid, int tryTimes) throws InterruptedException {
        int times = 0;
        String res = null;
        while (times < tryTimes) {
            try {
                res = wxBotService.waitForLogin(uuid);
                if (res != null) {
                    break;
                }
            } catch (Exception e) {
                tryTimes++;
                Thread.sleep(1000L * times);
            }
        }
        if (times >= tryTimes) {
            throw new RuntimeException("等待扫描二维码超时！");
        }
        return res;
    }

}
