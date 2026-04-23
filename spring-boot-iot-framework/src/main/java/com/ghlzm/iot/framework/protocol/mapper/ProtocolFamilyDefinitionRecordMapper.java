package com.ghlzm.iot.framework.protocol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProtocolFamilyDefinitionRecordMapper extends BaseMapper<ProtocolFamilyDefinitionRecord> {

    @Select("""
            SELECT id, tenant_id, family_code, protocol_code, display_name, decrypt_profile_code,
                   sign_algorithm, normalization_strategy, status, version_no, approval_order_id,
                   create_by, create_time, update_by, update_time, deleted
            FROM iot_protocol_family_definition
            WHERE deleted = 0
              AND family_code = #{familyCode}
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolFamilyDefinitionRecord selectLatestByFamilyCode(@Param("familyCode") String familyCode);

    @Select("""
            SELECT id, tenant_id, family_code, protocol_code, display_name, decrypt_profile_code,
                   sign_algorithm, normalization_strategy, status, version_no, approval_order_id,
                   create_by, create_time, update_by, update_time, deleted
            FROM iot_protocol_family_definition
            WHERE deleted = 0
              AND family_code = #{familyCode}
              AND status IN ('DRAFT', 'ACTIVE')
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolFamilyDefinitionRecord selectLatestEnabledByFamilyCode(@Param("familyCode") String familyCode);
}
