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

    @Select("")
    Integer getplatform_typeByShopId(Integer shopId);
}
