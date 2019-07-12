package com.dodoca.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dodoca.utils.AESUtil;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import com.whalin.MemCached.MemCachedClient;
import de.ailis.pherialize.Mixed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private MemCachedClient memCachedClient;

    public Map<Object,Object> getCacheInfo(String wxrrdWapSession) throws Exception {
        Map<Object,Object> result = new HashMap<>();
        if (wxrrdWapSession == null || wxrrdWapSession.isEmpty()) {
            return result;
        }
        //session信息base64解密
        JSONObject jsonObject = JSON.parseObject(new String(Base64.decode(wxrrdWapSession)));
        String value = jsonObject.getString("value");
        String iv = jsonObject.getString("iv");
        //获取解密后的memcache的key
        String cacheId = unserialize(AESUtil.decrypt(value, iv)).toString();
        //取 memcache 的数据
        String cacheInfo = memCachedClient.get("laravel:" + cacheId).toString();
        if (cacheInfo == null) {
            return result;
        }
        return (Map<Object, Object>) unserialize(cacheInfo).getValue();
    }



}
