package com.ghlzm.iot.common.event.governance;

/**
 * 产品对象洞察监测数据选择发生变化后，通知风险目录按最新正式批次或历史正式字段现状重建。
 */
public record ProductObjectInsightMetricsChangedEvent(
        Long tenantId,
        Long productId,
        Long releaseBatchId,
        Long operatorUserId
) {
}
