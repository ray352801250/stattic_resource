package com.dodoca.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Author: TianGuangHui
 * @Date: 2019/4/26 18:37
 * @Description:
 */
@Mapper
public interface ShopMapper {

    /**
     * 根据主键获取指定商铺的类型
     * 10:标准版；20:厂家总店；21:厂家子店；30:商超总店；31:商超子店；40:平台总店；41:平台子店'
     * @param shopId
     * @return
     */
    @Select("select platform_type from wxrrd.shop where id = #{shopId}")
    Integer getPlatformTypeById(Integer shopId);

    /**
     * 根据域名获取指定商铺的类型
     *      * 10:标准版；20:厂家总店；21:厂家子店；30:商超总店；31:商超子店；40:平台总店；41:平台子店'
     * @param subDomain 二级域名
     * @return
     */
    @Select("select platform_type from wxrrd.shop where subdomain = #{subDomain}")
    Integer getPlatformTypeBySubDomain(String subDomain);
}
