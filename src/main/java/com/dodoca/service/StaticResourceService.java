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


    /**
     * 获取指定redis中 的指定key
     * @param key
     * @param database redis: 0缓存数据源  1配置数据源
     * @return
     */
    JSONObject getKey(String key, Integer database);

    /**
     * 添加指定redis中 的指定key
     * @param key
     * @param value
     * @param database redis: null缓存数据源  int配置数据源
     * @return
     */
    JSONObject setKey(String key, String value, Integer database);

    /**
     * 删除指定redis中 的指定key
     * @param key
     * @param database redis: null缓存数据源  int配置数据源
     * @return
     */
    JSONObject delKey(String key, Integer database);

    /**
     * 获取指定redis中 的指定key 的过期时间
     * @param key
     * @param database redis: null缓存数据源  int配置数据源
     * @return
     */
    JSONObject getExpire(String key, Integer database);


    /**
     * 获取指定redis中 的指定key
     * @param key
     * @param database redis: 0缓存数据源  1配置数据源
     * @return
     */
    JSONObject hgetKey(String key, String filed, Integer database);

    /**
     *
     * @param key
     * @param value
     * @param database redis: null缓存数据源  int配置数据源
     * @return
     */
    JSONObject hsetKey(String key, String filed, String value, Integer database);

    /**
     * 删除指定redis中 的指定key
     * @param key
     * @param filed
     * @param database redis: null(0)缓存数据源  int配置数据源
     * @return
     */
    JSONObject hdelKey(String key, String filed, Integer database);
}
