package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductModelCandidateConfirmDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelManualExtractDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.service.ProductModelService;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
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

    @GetMapping("/api/device/product/{productId}/model-candidates")
    public R<ProductModelCandidateResultVO> listCandidates(@PathVariable Long productId) {
        return R.ok(productModelService.listModelCandidates(productId));
    }

    @PostMapping("/api/device/product/{productId}/model-candidates/manual-extract")
    public R<ProductModelCandidateResultVO> manualExtract(@PathVariable Long productId,
                                                          @RequestBody @Valid ProductModelManualExtractDTO dto) {
        return R.ok(productModelService.manualExtractModelCandidates(productId, dto));
    }

    @PostMapping("/api/device/product/{productId}/model-candidates/confirm")
    public R<ProductModelCandidateSummaryVO> confirmCandidates(@PathVariable Long productId,
                                                               @RequestBody ProductModelCandidateConfirmDTO dto) {
        return R.ok(productModelService.confirmModelCandidates(productId, dto));
    }

    @PostMapping("/api/device/product/{productId}/model-governance/compare")
    public R<ProductModelGovernanceCompareVO> compareGovernance(@PathVariable Long productId,
                                                                @RequestBody ProductModelGovernanceCompareDTO dto) {
        return R.ok(productModelService.compareGovernance(productId, dto));
    }

    @PostMapping("/api/device/product/{productId}/model-governance/apply")
    public R<ProductModelGovernanceApplyResultVO> applyGovernance(@PathVariable Long productId,
                                                                  @RequestBody ProductModelGovernanceApplyDTO dto) {
        return R.ok(productModelService.applyGovernance(productId, dto));
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
