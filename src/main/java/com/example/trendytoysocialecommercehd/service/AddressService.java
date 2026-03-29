package com.example.trendytoysocialecommercehd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.trendytoysocialecommercehd.dto.CreateAddressRequest;
import com.example.trendytoysocialecommercehd.dto.UpdateAddressRequest;
import com.example.trendytoysocialecommercehd.entity.UserAddress;
import com.example.trendytoysocialecommercehd.mapper.UserAddressMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    public List<UserAddress> getAddressesByUserId(String userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId);
        wrapper.orderByDesc(UserAddress::getIsDefault);
        wrapper.orderByDesc(UserAddress::getCreateTime);
        return userAddressMapper.selectList(wrapper);
    }

    public UserAddress getAddressById(String addressId) {
        return userAddressMapper.selectById(addressId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAddress createAddress(String userId, CreateAddressRequest request) {
        String addressId = UUID.randomUUID().toString();

        // 如果设置为默认地址，先取消其他默认地址
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            cancelDefaultAddress(userId);
        }

        UserAddress address = new UserAddress();
        address.setAddressId(addressId);
        address.setUserId(userId);
        address.setRecipientName(request.getRecipientName());
        address.setRecipientPhone(request.getRecipientPhone());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "中国");
        address.setProvince(request.getProvince());
        address.setCity(request.getCity());
        address.setDistrict(request.getDistrict());
        address.setStreet(request.getStreet());
        address.setDetailAddress(request.getDetailAddress());
        address.setFullAddress(buildFullAddress(request));
        address.setPostalCode(request.getPostalCode());
        address.setAddressTag(request.getAddressTag());
        address.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : false);
        address.setLongitude(request.getLongitude());
        address.setLatitude(request.getLatitude());
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());

        userAddressMapper.insert(address);
        return address;
    }

    @Transactional(rollbackFor = Exception.class)
    public UserAddress updateAddress(String userId, UpdateAddressRequest request) {
        UserAddress address = userAddressMapper.selectById(request.getAddressId());
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        if (!address.getUserId().equals(userId)) {
            throw new RuntimeException("无权修改此地址");
        }

        // 如果设置为默认地址，先取消其他默认地址
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            cancelDefaultAddress(userId);
        }

        if (request.getRecipientName() != null) {
            address.setRecipientName(request.getRecipientName());
        }
        if (request.getRecipientPhone() != null) {
            address.setRecipientPhone(request.getRecipientPhone());
        }
        if (request.getCountry() != null) {
            address.setCountry(request.getCountry());
        }
        if (request.getProvince() != null) {
            address.setProvince(request.getProvince());
        }
        if (request.getCity() != null) {
            address.setCity(request.getCity());
        }
        if (request.getDistrict() != null) {
            address.setDistrict(request.getDistrict());
        }
        if (request.getStreet() != null) {
            address.setStreet(request.getStreet());
        }
        if (request.getDetailAddress() != null) {
            address.setDetailAddress(request.getDetailAddress());
        }
        if (request.getPostalCode() != null) {
            address.setPostalCode(request.getPostalCode());
        }
        if (request.getAddressTag() != null) {
            address.setAddressTag(request.getAddressTag());
        }
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }
        if (request.getLongitude() != null) {
            address.setLongitude(request.getLongitude());
        }
        if (request.getLatitude() != null) {
            address.setLatitude(request.getLatitude());
        }

        address.setFullAddress(buildFullAddressFromEntity(address));
        address.setUpdateTime(LocalDateTime.now());

        userAddressMapper.updateById(address);
        return address;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAddress(String userId, String addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        if (!address.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此地址");
        }

        userAddressMapper.deleteById(addressId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAddress(String userId, String addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null) {
            throw new RuntimeException("地址不存在");
        }

        if (!address.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此地址");
        }

        // 取消其他默认地址
        cancelDefaultAddress(userId);

        // 设置当前地址为默认
        address.setIsDefault(true);
        address.setUpdateTime(LocalDateTime.now());
        userAddressMapper.updateById(address);
    }

    private void cancelDefaultAddress(String userId) {
        LambdaUpdateWrapper<UserAddress> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(UserAddress::getUserId, userId);
        updateWrapper.set(UserAddress::getIsDefault, false);
        userAddressMapper.update(null, updateWrapper);
    }

    private String buildFullAddress(CreateAddressRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getCountry() != null) {
            sb.append(request.getCountry());
        }
        if (request.getProvince() != null) {
            sb.append(request.getProvince());
        }
        if (request.getCity() != null) {
            sb.append(request.getCity());
        }
        if (request.getDistrict() != null) {
            sb.append(request.getDistrict());
        }
        if (request.getStreet() != null) {
            sb.append(request.getStreet());
        }
        if (request.getDetailAddress() != null) {
            sb.append(request.getDetailAddress());
        }
        return sb.toString();
    }

    private String buildFullAddressFromEntity(UserAddress address) {
        StringBuilder sb = new StringBuilder();
        if (address.getCountry() != null) {
            sb.append(address.getCountry());
        }
        if (address.getProvince() != null) {
            sb.append(address.getProvince());
        }
        if (address.getCity() != null) {
            sb.append(address.getCity());
        }
        if (address.getDistrict() != null) {
            sb.append(address.getDistrict());
        }
        if (address.getStreet() != null) {
            sb.append(address.getStreet());
        }
        if (address.getDetailAddress() != null) {
            sb.append(address.getDetailAddress());
        }
        return sb.toString();
    }
}