package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.service.ProductContractReleaseService;
import com.ghlzm.iot.device.vo.ProductContractReleaseBatchVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品契约发布批次控制器。
 */
@RestController
public class ProductContractReleaseController {

    private final ProductContractReleaseService productContractReleaseService;

    public ProductContractReleaseController(ProductContractReleaseService productContractReleaseService) {
        this.productContractReleaseService = productContractReleaseService;
    }

    @GetMapping("/api/device/product/{productId}/contract-release-batches")
    public R<PageResult<ProductContractReleaseBatchVO>> pageBatches(@PathVariable Long productId,
                                                                    @RequestParam(required = false) Long pageNum,
                                                                    @RequestParam(required = false) Long pageSize) {
        return R.ok(productContractReleaseService.pageBatches(productId, pageNum, pageSize));
    }

    @GetMapping("/api/device/product/contract-release-batches/{batchId}")
    public R<ProductContractReleaseBatchVO> getBatch(@PathVariable Long batchId) {
        return R.ok(productContractReleaseService.getBatch(batchId));
    }
}
