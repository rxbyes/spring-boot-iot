package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import com.ghlzm.iot.device.vo.ProductPageVO;
import java.util.List;

/**
 * 产品服务，负责一期产品最小管理能力。
 */
public interface ProductService extends IService<Product> {

    /**
     * 新增产品。
     */
    ProductDetailVO addProduct(ProductAddDTO dto);

    /**
     * 按主键查询产品，不存在时抛业务异常。
     */
    Product getRequiredById(Long id);

    /**
     * 按主键查询产品详情。
     */
    ProductDetailVO getDetailById(Long id);

    /**
     * 分页查询产品台账。
     */
    PageResult<ProductPageVO> pageProducts(String productKey,
                                           String productName,
                                           String protocolCode,
                                           Integer nodeType,
                                           Integer status,
                                           Long pageNum,
                                           Long pageSize);

    /**
     * 更新产品主数据。
     */
    ProductDetailVO updateProduct(Long id, ProductAddDTO dto);

    /**
     * 删除单个产品。
     */
    void deleteProduct(Long id);

    /**
     * 查询产品列表，供设备建档和工作台下拉框使用。
     */
    List<Product> listAvailableProducts();

    /**
     * 按 productKey 查询产品，不存在时抛业务异常。
     */
    Product getRequiredByProductKey(String productKey);
}
