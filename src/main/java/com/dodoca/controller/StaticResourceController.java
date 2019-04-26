package com.dodoca.controller;


import com.alibaba.fastjson.JSONObject;
import com.dodoca.service.StaticResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;


/**
 * @Author: TianGuangHui
 * @Date: 2019/4/24 14:38
 * @Description:
 */
@Controller
@RequestMapping("/static/")
public class StaticResourceController {
    private static final Logger logger = LoggerFactory.getLogger(StaticResourceController.class);

    @Autowired
    StaticResourceService staticResourceServiceImpl;


    @RequestMapping("/resourceTwo")
    @ResponseBody
    public JSONObject resourceController(HttpServletRequest request) {
        return staticResourceServiceImpl.homePageResource(request);
    }


    @RequestMapping("/phpStockInterface")
    @ResponseBody
    public JSONObject phpStockInterface(HttpServletRequest request) {
        return staticResourceServiceImpl.phpStockInterface(request);
    }







}
