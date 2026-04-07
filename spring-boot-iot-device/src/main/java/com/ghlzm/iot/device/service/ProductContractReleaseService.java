package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import com.ghlzm.iot.device.vo.ProductContractReleaseRollbackResultVO;

/**
 * Contract release batch service.
 */
public interface ProductContractReleaseService {

    Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId);

    void saveBatchSnapshot(Long batchId,
                           Long productId,
                           String snapshotStage,
                           String snapshotJson,
                           Long operatorId);

    PageResult<ProductContractReleaseBatchVO> pageBatches(Long productId, Long pageNum, Long pageSize);

    ProductContractReleaseBatchVO getBatch(Long batchId);

    ProductContractReleaseRollbackResultVO rollbackLatestBatch(Long batchId, Long operatorId);
}
