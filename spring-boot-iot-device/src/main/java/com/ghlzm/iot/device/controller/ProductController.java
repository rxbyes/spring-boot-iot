package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Author rxbyes
 * Since 2.0
 * Date 2026/3/13 - 14:00
 */
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/device/product/add")
    public R<?> add(@RequestBody @Valid ProductAddDTO dto) {
        Product product = new Product();
        BeanUtils.copyProperties(dto, product);
        product.setStatus(1);
        productService.save(product);
        return R.ok();
    }

    @GetMapping("/device/product/{id}")
    public R<Product> getById(@PathVariable Long id) {
        return R.ok(productService.getById(id));
    }
}

