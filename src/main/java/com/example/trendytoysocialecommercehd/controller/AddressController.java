package com.example.trendytoysocialecommercehd.controller;

import com.example.trendytoysocialecommercehd.common.Result;
import com.example.trendytoysocialecommercehd.dto.CreateAddressRequest;
import com.example.trendytoysocialecommercehd.dto.UpdateAddressRequest;
import com.example.trendytoysocialecommercehd.entity.UserAddress;
import com.example.trendytoysocialecommercehd.service.AddressService;
import com.example.trendytoysocialecommercehd.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Autowired
    private AddressService addressService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/user/{userId}")
    public Result<List<UserAddress>> getUserAddresses(@PathVariable String userId) {
        try {
            List<UserAddress> addresses = addressService.getAddressesByUserId(userId);
            return Result.success(addresses);
        } catch (Exception e) {
            return Result.error("获取地址列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{addressId}")
    public Result<UserAddress> getAddressDetail(@PathVariable String addressId) {
        try {
            UserAddress address = addressService.getAddressById(addressId);
            if (address == null) {
                return Result.error("地址不存在");
            }
            return Result.success(address);
        } catch (Exception e) {
            return Result.error("获取地址详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<UserAddress> createAddress(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateAddressRequest request) {
        try {
            String userId = getUserIdFromToken(token);
            UserAddress address = addressService.createAddress(userId, request);
            return Result.success(address);
        } catch (Exception e) {
            return Result.error("创建地址失败: " + e.getMessage());
        }
    }

    @PutMapping
    public Result<UserAddress> updateAddress(
            @RequestHeader("Authorization") String token,
            @RequestBody UpdateAddressRequest request) {
        try {
            String userId = getUserIdFromToken(token);
            UserAddress address = addressService.updateAddress(userId, request);
            return Result.success(address);
        } catch (Exception e) {
            return Result.error("更新地址失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{addressId}")
    public Result<Void> deleteAddress(
            @RequestHeader("Authorization") String token,
            @PathVariable String addressId) {
        try {
            String userId = getUserIdFromToken(token);
            addressService.deleteAddress(userId, addressId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("删除地址失败: " + e.getMessage());
        }
    }

    @PutMapping("/{addressId}/default")
    public Result<Void> setDefaultAddress(
            @RequestHeader("Authorization") String token,
            @PathVariable String addressId) {
        try {
            String userId = getUserIdFromToken(token);
            addressService.setDefaultAddress(userId, addressId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error("设置默认地址失败: " + e.getMessage());
        }
    }

    private String getUserIdFromToken(String token) {
        String cleanToken = token.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(cleanToken);
    }
}