package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.entity.Product;
import com.example.trendytoysocialecommercehd.mapper.ProductMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "图鉴产品")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/{productId}")
    @Operation(summary = "获取产品详情")
    public Result<Product> getProductDetail(@PathVariable String productId) {
        try {
            Product product = productMapper.selectById(productId);
            if (product == null) {
                return Result.error("产品不存在");
            }
            return Result.success(product);
        } catch (Exception e) {
            return Result.error("获取产品详情失败: " + e.getMessage());
        }
    }
}
