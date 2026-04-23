package com.ghlzm.iot.framework.protocol.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.framework.protocol.entity.ProtocolDecryptProfileSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProtocolDecryptProfileSnapshotMapper extends BaseMapper<ProtocolDecryptProfileSnapshot> {

    @Select("""
            <script>
            SELECT id, tenant_id, profile_id, approval_order_id, published_version_no,
                   snapshot_json, lifecycle_status, create_by, create_time, deleted
            FROM iot_protocol_decrypt_profile_snapshot
            WHERE deleted = 0
              AND lifecycle_status = 'PUBLISHED'
              AND profile_id IN
              <foreach collection="profileIds" item="profileId" open="(" separator="," close=")">
                #{profileId}
              </foreach>
            ORDER BY published_version_no DESC, id DESC
            </script>
            """)
    List<ProtocolDecryptProfileSnapshot> selectPublishedByProfileIds(@Param("profileIds") List<Long> profileIds);

    @Select("""
            SELECT id, tenant_id, profile_id, approval_order_id, published_version_no,
                   snapshot_json, lifecycle_status, create_by, create_time, deleted
            FROM iot_protocol_decrypt_profile_snapshot
            WHERE deleted = 0
              AND profile_id = #{profileId}
              AND lifecycle_status = 'PUBLISHED'
            ORDER BY published_version_no DESC, id DESC
            LIMIT 1
            """)
    ProtocolDecryptProfileSnapshot selectLatestPublishedByProfileId(@Param("profileId") Long profileId);
}
