package com.ghlzm.iot.framework.protocol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProtocolDecryptProfileRecordMapper extends BaseMapper<ProtocolDecryptProfileRecord> {

    @Select("""
            SELECT id, tenant_id, profile_code, algorithm, merchant_source, merchant_key,
                   transformation, signature_secret, status, version_no, approval_order_id,
                   create_by, create_time, update_by, update_time, deleted
            FROM iot_protocol_decrypt_profile
            WHERE deleted = 0
              AND profile_code = #{profileCode}
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolDecryptProfileRecord selectLatestByProfileCode(@Param("profileCode") String profileCode);

    @Select("""
            SELECT id, tenant_id, profile_code, algorithm, merchant_source, merchant_key,
                   transformation, signature_secret, status, version_no, approval_order_id,
                   create_by, create_time, update_by, update_time, deleted
            FROM iot_protocol_decrypt_profile
            WHERE deleted = 0
              AND profile_code = #{profileCode}
              AND status IN ('DRAFT', 'ACTIVE')
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolDecryptProfileRecord selectLatestEnabledByProfileCode(@Param("profileCode") String profileCode);

    @Select("""
            SELECT id, tenant_id, profile_code, algorithm, merchant_source, merchant_key,
                   transformation, signature_secret, status, version_no, approval_order_id,
                   create_by, create_time, update_by, update_time, deleted
            FROM iot_protocol_decrypt_profile
            WHERE deleted = 0
              AND merchant_key = #{merchantKey}
              AND status IN ('DRAFT', 'ACTIVE')
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolDecryptProfileRecord selectLatestEnabledByMerchantKey(@Param("merchantKey") String merchantKey);
}
