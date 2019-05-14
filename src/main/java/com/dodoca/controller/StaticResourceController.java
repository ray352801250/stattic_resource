package com.dodoca.controller;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.service.StaticResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/24 14:38
 * @Description:
 */
@Controller
@RequestMapping("/static")
public class StaticResourceController {

    @Autowired
    StaticResourceService staticResourceServiceImpl;

    /**
     * 首页请求
     * @param request
     * @return
     */
    @RequestMapping("/homePageResource")
    @ResponseBody
    public JSONObject homePageResource(HttpServletRequest request) {
        return staticResourceServiceImpl.homePageResource(request);
    }

    /**
     * 商品详情页请求
     * @param request
     * @return
     */
    @RequestMapping("/goodsDetailResource")
    @ResponseBody
    public JSONObject goodsDetailResource(HttpServletRequest request) {
        return staticResourceServiceImpl.goodsDetailResource(request);
    }


    /**
     * 测试库存
     * @param request
     * @return
     */
    @RequestMapping("/phpStockInterface")
    @ResponseBody
    public JSONObject phpStockInterface(HttpServletRequest request) {
        return staticResourceServiceImpl.phpStockInterface(request);
    }


    @RequestMapping("/getKey")
    @ResponseBody
    public JSONObject getKey(String key, Integer database) {
        return staticResourceServiceImpl.getKey(key, database);
    }


    @RequestMapping("/setKey")
    @ResponseBody
    public JSONObject setKey(String key, String value, Integer database) {
        return staticResourceServiceImpl.setKey(key, value, database);
    }

    @RequestMapping("/delKey")
    @ResponseBody
    public JSONObject delKey(String key, Integer database) {
        return staticResourceServiceImpl.delKey(key, database);
    }

    @RequestMapping("/getExpire")
    @ResponseBody
    public JSONObject getExpire(String key, Integer database) {
        return staticResourceServiceImpl.getExpire(key, database);
    }

    @RequestMapping("/hgetKey")
    @ResponseBody
    public JSONObject hgetKey(String key, String filed, Integer database) {
        return staticResourceServiceImpl.hgetKey(key, filed, database);
    }


    @RequestMapping("/hsetKey")
    @ResponseBody
    public JSONObject hsetKey(String key, String filed, String value, Integer database) {
        return staticResourceServiceImpl.hsetKey(key, filed, value, database);
    }

    @RequestMapping("/hdelKey")
    @ResponseBody
    public JSONObject hdelKey(String key, String filed, Integer database) {
        return staticResourceServiceImpl.hdelKey(key, filed, database);
    }


}
