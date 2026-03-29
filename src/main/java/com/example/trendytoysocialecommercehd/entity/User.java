package com.example.trendytoysocialecommercehd.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_UUID)
    private String userId;
    private String username;
    private String passwordHash;
    private String phoneNumber;
    private String email;
    private String gender;
    private Date birthDate;
    private String location;
    private String bio;
    private String avatarUrl;
    private String accountStatus;
    private Integer accountLevel;
    private String membershipType;
    private Integer totalOrders;
    private BigDecimal totalSpent;
    private Integer totalLoginCount;
    private Integer consecutiveLoginDays;
    private Integer postCount;
    private Integer followingCount;
    private Integer followerCount;
    private Integer totalLikesReceived;
    private Integer favoriteProductCount;
    private Integer couponCount;
    private Integer cabinetCount;
    private List<String> favoriteIps;
    private Date registerTime;
    private Date lastLoginTime;
}
