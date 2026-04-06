package com.ghlzm.iot.device.service;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;

/**
 * 契约发布批次服务。
 */
public interface ProductContractReleaseService {

    Long createBatch(Long productId, String scenarioCode, String releaseSource, int releasedFieldCount, Long operatorId);

    PageResult<ProductContractReleaseBatchVO> pageBatches(Long productId, Long pageNum, Long pageSize);

    ProductContractReleaseBatchVO getBatch(Long batchId);
}
