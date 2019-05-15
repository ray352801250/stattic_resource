package com.dodoca.service;

import com.alibaba.fastjson.JSONObject;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 09:49
 * @Description:
 */
public interface StaticResourceVersionOneService {

    /**
     * 首页请求
     * @param request
     * @return
     */
    JSONObject resource(HttpServletRequest request);

    /**
     * 详情页请求
     * @param request
     * @return
     */
    JSONObject resourceGoods(HttpServletRequest request);
}
