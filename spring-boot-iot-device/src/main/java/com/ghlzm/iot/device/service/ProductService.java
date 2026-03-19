package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import java.util.List;

/**
 * 产品服务，负责一期产品最小管理能力。
 */
public interface ProductService extends IService<Product> {

    /**
     * 新增产品。
     */
    Product addProduct(ProductAddDTO dto);

    /**
     * 按主键查询产品，不存在时抛业务异常。
     */
    Product getRequiredById(Long id);

    /**
     * 查询产品列表，供设备建档和工作台下拉框使用。
     */
    List<Product> listAvailableProducts();

    /**
     * 按 productKey 查询产品，不存在时抛业务异常。
     */
    Product getRequiredByProductKey(String productKey);
}
