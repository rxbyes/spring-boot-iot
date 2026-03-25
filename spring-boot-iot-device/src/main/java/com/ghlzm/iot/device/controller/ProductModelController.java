package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 产品物模型控制器，仅负责产品维度物模型请求的收发。
 */
@RestController
public class ProductModelController {

    private final ProductModelService productModelService;

    public ProductModelController(ProductModelService productModelService) {
        this.productModelService = productModelService;
    }

    @GetMapping("/api/device/product/{productId}/models")
    public R<List<ProductModelVO>> list(@PathVariable Long productId) {
        return R.ok(productModelService.listModels(productId));
    }

    @PostMapping("/api/device/product/{productId}/models")
    public R<ProductModelVO> add(@PathVariable Long productId, @RequestBody @Valid ProductModelUpsertDTO dto) {
        return R.ok(productModelService.createModel(productId, dto));
    }

    @PutMapping("/api/device/product/{productId}/models/{modelId}")
    public R<ProductModelVO> update(@PathVariable Long productId,
                                    @PathVariable Long modelId,
                                    @RequestBody @Valid ProductModelUpsertDTO dto) {
        return R.ok(productModelService.updateModel(productId, modelId, dto));
    }

    @DeleteMapping("/api/device/product/{productId}/models/{modelId}")
    public R<Void> delete(@PathVariable Long productId, @PathVariable Long modelId) {
        productModelService.deleteModel(productId, modelId);
        return R.ok();
    }
}
