package com.dodoca.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.dodoca.service.StaticResourceVersionTwoService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 10:17
 * @Description:
 */
@Service
public class StaticResourceVersionTwoServiceImpl implements StaticResourceVersionTwoService {

    @Override
    public JSONObject resource(HttpServletRequest request) {
        return null;
    }

    @Override
    public JSONObject resourceGoods(HttpServletRequest request) {
        return null;
    }
}
