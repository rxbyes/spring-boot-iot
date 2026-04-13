package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.ProductModel;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductModelMapper extends BaseMapper<ProductModel> {

    @Select("""
            SELECT id, tenant_id, product_id, model_type, identifier, model_name, data_type,
                   specs_json, event_type, service_input_json, service_output_json,
                   sort_no, required_flag, description, create_time, update_time, deleted
            FROM iot_product_model
            WHERE product_id = #{productId}
              AND identifier = #{identifier}
            ORDER BY deleted ASC, id ASC
            """)
    List<ProductModel> selectAnyByProductAndIdentifier(@Param("productId") Long productId,
                                                       @Param("identifier") String identifier);

    @Update("""
            UPDATE iot_product_model
            SET tenant_id = COALESCE(#{model.tenantId}, tenant_id),
                product_id = #{model.productId},
                model_type = #{model.modelType},
                identifier = #{model.identifier},
                model_name = #{model.modelName},
                data_type = #{model.dataType},
                specs_json = #{model.specsJson},
                event_type = #{model.eventType},
                service_input_json = #{model.serviceInputJson},
                service_output_json = #{model.serviceOutputJson},
                sort_no = #{model.sortNo},
                required_flag = #{model.requiredFlag},
                description = #{model.description},
                deleted = 0,
                update_time = NOW()
            WHERE id = #{model.id}
            """)
    int reviveDeletedById(@Param("model") ProductModel model);

    @Delete("DELETE FROM iot_product_model WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);
}
