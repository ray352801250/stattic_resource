package com.dodoca.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 查询商品的营销活动
 * @Author: TianGuangHui
 * @Date: 2019/5/20 15:03
 * @Description:
 */
@Mapper
public interface AloneActivityRecodeMapper {


    /**
     * 查询指定商品当前参与的活动类型
     * @param goodsId 商品id
     * @param dateTime 当前时间字符串
     * @return
     */
    @Select("select act_type from alone_activity_recode where goods_id = #{goodsId} and created_at <= #{dateTime} and (finish_at = '0000-00-00 00:00:00' or finish_at > #{dateTime})")
    String getActType(Integer goodsId, String dateTime);

}
