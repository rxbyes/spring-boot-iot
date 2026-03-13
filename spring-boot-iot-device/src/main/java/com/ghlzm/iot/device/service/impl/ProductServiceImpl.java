package com.ghlzm.iot.device.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.mapper.ProductMapper;
import com.ghlzm.iot.device.service.ProductService;
import org.springframework.stereotype.Service;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 13:58
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
}
