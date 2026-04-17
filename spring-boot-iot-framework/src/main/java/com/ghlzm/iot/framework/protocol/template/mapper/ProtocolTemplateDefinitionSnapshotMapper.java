package com.ghlzm.iot.framework.protocol.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.framework.protocol.template.entity.ProtocolTemplateDefinitionSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProtocolTemplateDefinitionSnapshotMapper extends BaseMapper<ProtocolTemplateDefinitionSnapshot> {

    @Select("""
            <script>
            SELECT id, tenant_id, template_id, template_code, family_code, protocol_code,
                   published_version_no, snapshot_json, lifecycle_status, approval_order_id,
                   submit_reason, create_by, create_time, deleted
            FROM iot_protocol_template_definition_snapshot
            WHERE deleted = 0
              AND lifecycle_status = 'PUBLISHED'
              AND template_id IN
              <foreach collection="templateIds" item="templateId" open="(" separator="," close=")">
                #{templateId}
              </foreach>
            ORDER BY published_version_no DESC, id DESC
            </script>
            """)
    List<ProtocolTemplateDefinitionSnapshot> selectPublishedByTemplateIds(@Param("templateIds") List<Long> templateIds);

    @Select("""
            SELECT id, tenant_id, template_id, template_code, family_code, protocol_code,
                   published_version_no, snapshot_json, lifecycle_status, approval_order_id,
                   submit_reason, create_by, create_time, deleted
            FROM iot_protocol_template_definition_snapshot
            WHERE deleted = 0
              AND template_id = #{templateId}
              AND lifecycle_status = 'PUBLISHED'
            ORDER BY published_version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolTemplateDefinitionSnapshot selectLatestPublishedByTemplateId(@Param("templateId") Long templateId);
}
