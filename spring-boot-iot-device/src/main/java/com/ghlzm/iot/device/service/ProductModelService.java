package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.device.dto.ProductModelCandidateConfirmDTO;
import com.ghlzm.iot.device.dto.ProductModelManualExtractDTO;
import com.ghlzm.iot.device.dto.ProductModelUpsertDTO;
import com.ghlzm.iot.device.entity.ProductModel;
import com.ghlzm.iot.device.vo.ProductModelCandidateResultVO;
import com.ghlzm.iot.device.vo.ProductModelCandidateSummaryVO;
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
     * 基于真实上报数据提炼物模型候选。
     */
    ProductModelCandidateResultVO listModelCandidates(Long productId);

    /**
     * 基于单设备样本手动提炼物模型候选。
     */
    ProductModelCandidateResultVO manualExtractModelCandidates(Long productId, ProductModelManualExtractDTO dto);

    /**
     * 确认候选并写入正式物模型。
     */
    ProductModelCandidateSummaryVO confirmModelCandidates(Long productId, ProductModelCandidateConfirmDTO dto);

    /**
     * 更新物模型。
     */
    ProductModelVO updateModel(Long productId, Long modelId, ProductModelUpsertDTO dto);

    /**
     * 删除物模型。
     */
    void deleteModel(Long productId, Long modelId);
}
