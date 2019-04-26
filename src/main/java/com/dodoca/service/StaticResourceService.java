package com.dodoca.service;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 10:26
 * @Description:
 */
public interface StaticResourceService {

    /**
     * 首页请求
     * @param request
     * @return
     */
    JSONObject homePageResource(HttpServletRequest request);

    /**
     * 详情页请求
     * @param request
     * @return
     */
    JSONObject goodsDetailResource(HttpServletRequest request);

    /**
     * 模拟请求php的库存服务
     * @param request
     * @return
     */
    JSONObject phpStockInterface(HttpServletRequest request);
}
