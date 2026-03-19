package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.common.exception.BizException;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.ProductService;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 产品服务实现，只承载一期最小产品管理骨架。
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product addProduct(ProductAddDTO dto) {
        // 一期先按 productKey 做唯一性校验，避免重复创建同一产品。
        Product existing = lambdaQuery()
                .eq(Product::getProductKey, dto.getProductKey())
                .eq(Product::getDeleted, 0)
                .one();
        if (existing != null) {
            throw new BizException("产品Key已存在: " + dto.getProductKey());
        }

        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        // 数据格式和状态在最小骨架阶段给出默认值，避免控制层承担业务兜底。
        product.setDataFormat(StringUtils.hasText(dto.getDataFormat()) ? dto.getDataFormat() : "JSON");
        product.setStatus(1);
        save(product);
        return product;
    }

    @Override
    public Product getRequiredById(Long id) {
        // 查询接口统一复用“必须存在”语义，便于控制层直接返回结果。
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
    public List<Product> listAvailableProducts() {
        // 设备资产中心的产品下拉框依赖该列表接口，只返回未删除产品并保持新建优先排序。
        return lambdaQuery()
                .eq(Product::getDeleted, 0)
                .orderByDesc(Product::getCreateTime)
                .orderByDesc(Product::getId)
                .list();
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
