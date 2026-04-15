package com.ghlzm.iot.device.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ghlzm.iot.device.entity.VendorMetricMappingRuleSnapshot;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VendorMetricMappingRuleSnapshotMapper extends BaseMapper<VendorMetricMappingRuleSnapshot> {

    @Select("""
            SELECT id, tenant_id, rule_id, product_id, approval_order_id, published_version_no,
                   snapshot_json, lifecycle_status, create_by, create_time, deleted
            FROM iot_vendor_metric_mapping_rule_snapshot
            WHERE deleted = 0
              AND rule_id = #{ruleId}
              AND lifecycle_status = 'PUBLISHED'
            ORDER BY published_version_no DESC, id DESC
            LIMIT 1
            """)
    VendorMetricMappingRuleSnapshot selectLatestPublishedByRuleId(@Param("ruleId") Long ruleId);

    @Select("""
            SELECT id, tenant_id, rule_id, product_id, approval_order_id, published_version_no,
                   snapshot_json, lifecycle_status, create_by, create_time, deleted
            FROM iot_vendor_metric_mapping_rule_snapshot
            WHERE deleted = 0
              AND product_id = #{productId}
              AND lifecycle_status = 'PUBLISHED'
            ORDER BY published_version_no DESC, id DESC
            """)
    List<VendorMetricMappingRuleSnapshot> selectPublishedByProductId(@Param("productId") Long productId);
}
