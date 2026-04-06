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
 * 产品物模型服务。
 */
public interface ProductModelService extends IService<ProductModel> {

    /**
     * 按产品查询物模型列表。
     */
    List<ProductModelVO> listModels(Long productId);

    /**
     * 新增物模型。
     */
    ProductModelVO createModel(Long productId, ProductModelUpsertDTO dto);

    /**
     * 更新物模型。
     */
    ProductModelVO updateModel(Long productId, Long modelId, ProductModelUpsertDTO dto);

    /**
     * 基于手动样本构建契约字段 compare 结果。
     */
    default ProductModelGovernanceCompareVO compareGovernance(Long productId, ProductModelGovernanceCompareDTO dto) {
        throw new UnsupportedOperationException("compareGovernance 尚未实现");
    }

    /**
     * 应用双证据治理决策。
     */
    default ProductModelGovernanceApplyResultVO applyGovernance(Long productId, ProductModelGovernanceApplyDTO dto) {
        throw new UnsupportedOperationException("applyGovernance 尚未实现");
    }

    /**
     * 删除物模型。
     */
    void deleteModel(Long productId, Long modelId);
}
