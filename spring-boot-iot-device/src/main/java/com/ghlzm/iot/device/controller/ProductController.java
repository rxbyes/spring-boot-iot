package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * 产品控制器，只负责请求接收与响应返回。
 */
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/device/product/add")
    public R<Product> add(@RequestBody @Valid ProductAddDTO dto) {
        // 控制层不承载业务判断，直接交给服务层处理。
        return R.ok(productService.addProduct(dto));
    }

    @GetMapping("/device/product/{id}")
    public R<Product> getById(@PathVariable("id") Long id) {
        return R.ok(productService.getRequiredById(id));
    }
}
