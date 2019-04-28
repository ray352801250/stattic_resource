package com.dodoca.controller;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.service.StaticResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/static/")
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







}
