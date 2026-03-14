package com.ghlzm.iot.device.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;

public interface ProductService extends IService<Product> {

    Product addProduct(ProductAddDTO dto);

    Product getRequiredById(Long id);

    Product getRequiredByProductKey(String productKey);
}
