package com.ghlzm.iot.alarm.service;

import com.ghlzm.iot.device.entity.ProductModel;

import java.util.List;

/**
 * 统一的风险指标目录重建入口。
 */
public interface RiskMetricCatalogRebuildService {

    boolean rebuildReleasedContracts(Long productId, Long releaseBatchId, List<ProductModel> releasedContracts);

    /**
     * 优先按最新正式发布批次重建；若历史产品尚未形成批次，则按当前正式字段现状重建。
     */
    boolean rebuildLatestRelease(Long productId);
}
