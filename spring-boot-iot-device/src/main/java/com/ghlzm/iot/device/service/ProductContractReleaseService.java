package com.ghlzm.iot.device.service;

/**
 * 契约发布批次服务。
 */
public interface ProductContractReleaseService {

    Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId);
}
