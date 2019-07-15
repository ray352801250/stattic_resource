package com.dodoca.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dodoca.config.MemcachedRunner;
import com.dodoca.utils.AESUtil;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
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
    String encryptionKey = "QqRb4d2TlBcE0SY8xLycs6mMPUPpImeb";


    @Autowired
    private MemcachedRunner memcachedRunner;

    public Map<Object,Object> getCacheInfo(String wxrrdWapSession) throws Exception {
        Map<Object,Object> result = new HashMap<>();
        if (wxrrdWapSession == null || wxrrdWapSession.isEmpty()) {
            return result;
        }
        //session信息base64解密
        byte[] decode = Base64.decode(wxrrdWapSession);
        if (decode == null) {
            looger.info("base64解码为空");
            return result;
        }
        String wxrrdWapSessionBase64 = new String(decode);
        JSONObject jsonObject = JSON.parseObject(wxrrdWapSessionBase64);
        String value = jsonObject.getString("value");
        String iv = jsonObject.getString("iv");
        //获取解密后的memcache的key
        String cacheId = unserialize(AESUtil.decrypt(value, iv, encryptionKey)).toString();
        String cacheKey = "laravel:" + cacheId;
        looger.info("cacheKey: " + cacheKey);
        //取 memcache 的数据
        MemcachedClient memCachedClient = memcachedRunner.getClient();
        Object cacheInfo = memCachedClient.get(cacheKey);
        if (cacheInfo == null) {
            looger.info("从memcache取: " + cacheKey + " 为空");
            return result;
        }
        return (Map<Object, Object>) unserialize(cacheInfo.toString()).getValue();
    }



}
