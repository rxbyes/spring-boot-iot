package com.ghlzm.iot.framework.protocol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.framework.protocol.entity.ProtocolFamilyDefinitionSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProtocolFamilyDefinitionSnapshotMapper extends BaseMapper<ProtocolFamilyDefinitionSnapshot> {

    @Select("""
            <script>
            SELECT id, tenant_id, family_id, approval_order_id, published_version_no,
                   snapshot_json, lifecycle_status, create_by, create_time, deleted
            FROM iot_protocol_family_definition_snapshot
            WHERE deleted = 0
              AND lifecycle_status = 'PUBLISHED'
              AND family_id IN
              <foreach collection="familyIds" item="familyId" open="(" separator="," close=")">
                #{familyId}
              </foreach>
            ORDER BY published_version_no DESC, id DESC
            </script>
            """)
    List<ProtocolFamilyDefinitionSnapshot> selectPublishedByFamilyIds(@Param("familyIds") List<Long> familyIds);

    @Select("""
            SELECT id, tenant_id, family_id, approval_order_id, published_version_no,
                   snapshot_json, lifecycle_status, create_by, create_time, deleted
            FROM iot_protocol_family_definition_snapshot
            WHERE deleted = 0
              AND family_id = #{familyId}
              AND lifecycle_status = 'PUBLISHED'
            ORDER BY published_version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolFamilyDefinitionSnapshot selectLatestPublishedByFamilyId(@Param("familyId") Long familyId);
}
