package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.ProductService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product addProduct(ProductAddDTO dto) {
        Product existing = lambdaQuery()
                .eq(Product::getProductKey, dto.getProductKey())
                .eq(Product::getDeleted, 0)
                .one();
        if (existing != null) {
            throw new BizException("产品Key已存在: " + dto.getProductKey());
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setDataFormat(StringUtils.hasText(dto.getDataFormat()) ? dto.getDataFormat() : "JSON");
        product.setStatus(1);
        save(product);
        return product;
    }

    @Override
    public Product getRequiredById(Long id) {
        Product product = lambdaQuery()
                .eq(Product::getId, id)
                .eq(Product::getDeleted, 0)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + id);
        }
        return product;
    }

    @Override
    public Product getRequiredByProductKey(String productKey) {
        Product product = lambdaQuery()
                .eq(Product::getProductKey, productKey)
                .eq(Product::getDeleted, 0)
                .one();
        if (product == null) {
            throw new BizException("产品不存在: " + productKey);
        }
        return product;
    }
}
