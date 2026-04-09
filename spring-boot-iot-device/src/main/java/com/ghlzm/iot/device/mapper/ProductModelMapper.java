package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.ProductModel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProductModelMapper extends BaseMapper<ProductModel> {

    @Delete("DELETE FROM iot_product_model WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
