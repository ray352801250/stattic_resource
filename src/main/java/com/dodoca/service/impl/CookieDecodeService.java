package com.dodoca.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.MemcachedRunner;
import com.dodoca.utils.AESUtil;


import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static de.ailis.pherialize.Pherialize.unserialize;

/**
 * @description:
 * @author: TianGuangHui
 * @create: 2019-07-11 12:24
 **/
@Service
public class CookieDecodeService {
    private static final Logger looger = LoggerFactory.getLogger(CookieDecodeService.class);

    @Value("${encryption_key}")
    private String encryptionKey;

    @Resource
    private MemcachedRunner memcachedRunner;

    public Map<Object,Object> getCacheInfo(String wxrrdWapSession) {
        Map<Object,Object> result = new HashMap<>();
        if (wxrrdWapSession == null || wxrrdWapSession.isEmpty()) {
            return result;
        }
        //session信息base64解密
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] decode = decoder.decode(wxrrdWapSession);
        if (decode == null) {
            looger.info("base64解码为空");
            return result;
        }
        String wxrrdWapSessionBase64 = new String(decode);
        JSONObject jsonObject = JSON.parseObject(wxrrdWapSessionBase64);
        String value = jsonObject.getString("value");
        String iv = jsonObject.getString("iv");
        //获取解密后的memcache的key
        String cacheId = null;
        try {
            cacheId = unserialize(AESUtil.decrypt(value, iv, encryptionKey)).toString();
        } catch (Exception e) {
            looger.error(e.getMessage(), e);
            return result;
        }
        String cacheKey = "laravel:" + cacheId;
        //取 memcache 的数据
        MemcachedClient memCachedClient = memcachedRunner.getClient();
        Object cacheInfo = memCachedClient.get(cacheKey);
        if (cacheInfo == null) {
            MemcachedClient memCachedClient2 = memcachedRunner.getClient2();
            cacheInfo = memCachedClient2.get(cacheKey);
        }
        if (cacheInfo == null) {
            looger.info("从memcache取: " + cacheKey + " 为空");
            return result;
        }
        looger.info(">>>cacheKey: {}  >>>cacheInfo: {}", cacheKey, cacheInfo);
        return (Map<Object, Object>) unserialize(cacheInfo.toString()).getValue();
    }




    public  String addRepeatPurchaseSignToRequestUri(HttpServletRequest request) {
        String requestUri = request.getHeader("request_uri");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String wxrrdWapSession = null;
            for (Cookie c : cookies) {
                if (c.getName().startsWith("wxrrd_wap_session")) {
                    wxrrdWapSession = c.getValue();
                }
            }
            //根据访客的session信息判断访客是不是推客
            if (wxrrdWapSession != null) {
                try {
                    wxrrdWapSession = URLDecoder.decode(wxrrdWapSession, "GBK");
                } catch (UnsupportedEncodingException e) {
                    looger.error(e.getMessage(), e);
                    return requestUri;
                }
                Map<Object, Object> cacheInfo = getCacheInfo(wxrrdWapSession);
                if (cacheInfo.get("guider") != null) {
                    requestUri = requestUri + "&is_repeatPurchase=1";
                }
            }

        }
        return requestUri;
    }



}
