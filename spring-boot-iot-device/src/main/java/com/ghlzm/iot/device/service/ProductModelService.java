package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.device.dto.ProductModelGovernanceApplyDTO;
import com.ghlzm.iot.device.dto.ProductModelGovernanceCompareDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.vo.ProductModelGovernanceApplyResultVO;
import com.ghlzm.iot.device.vo.ProductModelGovernanceCompareVO;
import com.ghlzm.iot.device.vo.ProductModelVO;
import java.util.List;

/**
 * Product model service.
 */
public interface ProductModelService extends IService<ProductModel> {

    List<ProductModelVO> listModels(Long productId);

    ProductModelVO createModel(Long productId, ProductModelUpsertDTO dto);

    ProductModelVO updateModel(Long productId, Long modelId, ProductModelUpsertDTO dto);

    default ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
        throw new UnsupportedOperationException("compareGovernance not implemented");
    }

    default ProductModelGovernanceApplyResultVO applyGovernance(Long productId, ProductModelGovernanceApplyDTO dto) {
        return applyGovernance(productId, dto, null, null);
    }

    default ProductModelGovernanceApplyResultVO applyGovernance(Long productId,
                                                                ProductModelGovernanceApplyDTO dto,
                                                                Long operatorId) {
        return applyGovernance(productId, dto, operatorId, null);
    }

    default ProductModelGovernanceApplyResultVO applyGovernance(Long productId,
                                                                ProductModelGovernanceApplyDTO dto,
                                                                Long operatorId,
                                                                Long approvalOrderId) {
        throw new UnsupportedOperationException("applyGovernance(operator) not implemented");
    }

    void deleteModel(Long productId, Long modelId);
}
