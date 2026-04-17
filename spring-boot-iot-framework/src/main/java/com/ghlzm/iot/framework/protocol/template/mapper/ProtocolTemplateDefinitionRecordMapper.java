package com.ghlzm.iot.framework.protocol.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProtocolTemplateDefinitionRecordMapper extends BaseMapper<ProtocolTemplateDefinitionRecord> {

    @Select("""
            SELECT id, tenant_id, template_code, family_code, protocol_code, display_name,
                   expression_json, output_mapping_json, status, version_no, approval_order_id,
                   create_by, create_time, update_by, update_time, deleted
            FROM iot_protocol_template_definition
            WHERE deleted = 0
              AND template_code = #{templateCode}
            ORDER BY version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolTemplateDefinitionRecord selectLatestByTemplateCode(@Param("templateCode") String templateCode);
}
