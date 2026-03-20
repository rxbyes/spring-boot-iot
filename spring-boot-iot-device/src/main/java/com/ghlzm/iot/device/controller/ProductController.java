package com.ghlzm.iot.device.controller;

import com.ghlzm.iot.common.response.PageResult;
import com.ghlzm.iot.common.response.R;
import com.ghlzm.iot.device.dto.ProductAddDTO;
import com.ghlzm.iot.device.entity.Product;
import com.ghlzm.iot.device.service.ProductService;
import com.ghlzm.iot.device.vo.ProductDetailVO;
import com.ghlzm.iot.device.vo.ProductPageVO;
import jakarta.validation.Valid;
import java.util.List;
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

    @PostMapping("/api/device/product/add")
    public R<ProductDetailVO> add(@RequestBody @Valid ProductAddDTO dto) {
        // 控制层不承载业务判断，直接交给服务层处理。
        return R.ok(productService.addProduct(dto));
    }

    @GetMapping("/api/device/product/page")
    public R<PageResult<ProductPageVO>> page(@RequestParam(required = false) String productKey,
                                             @RequestParam(required = false) String productName,
                                             @RequestParam(required = false) String protocolCode,
                                             @RequestParam(required = false) Integer nodeType,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") Long pageNum,
                                             @RequestParam(defaultValue = "10") Long pageSize) {
        return R.ok(productService.pageProducts(
                productKey,
                productName,
                protocolCode,
                nodeType,
                status,
                pageNum,
                pageSize
        ));
    }

    @GetMapping("/api/device/product/list")
    public R<List<Product>> list() {
        return R.ok(productService.listAvailableProducts());
    }

    @GetMapping("/api/device/product/{id}")
    public R<ProductDetailVO> getById(@PathVariable Long id) {
        return R.ok(productService.getDetailById(id));
    }

    @PutMapping("/api/device/product/{id}")
    public R<ProductDetailVO> update(@PathVariable Long id, @RequestBody @Valid ProductAddDTO dto) {
        return R.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/api/device/product/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return R.ok();
    }
}
