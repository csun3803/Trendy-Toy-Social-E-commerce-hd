package com.example.trendytoysocialecommercehd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.trendytoysocialecommercehd.dto.LoginDTO;
import com.example.trendytoysocialecommercehd.dto.RegisterDTO;
import com.example.trendytoysocialecommercehd.entity.User;
import com.example.trendytoysocialecommercehd.mapper.CabinetItemMapper;
import com.example.trendytoysocialecommercehd.mapper.FollowRelationshipMapper;
import com.example.trendytoysocialecommercehd.mapper.SocialActivityMapper;
import com.example.trendytoysocialecommercehd.mapper.UserCouponMapper;
import com.example.trendytoysocialecommercehd.mapper.UserInteractionMapper;
import com.example.trendytoysocialecommercehd.mapper.UserMapper;
import com.example.trendytoysocialecommercehd.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private FollowRelationshipMapper followRelationshipMapper;

    @Autowired
    private SocialActivityMapper socialActivityMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CabinetItemMapper cabinetItemMapper;

    @Autowired
    private UserInteractionMapper userInteractionMapper;

    @Override
    public User login(LoginDTO loginDTO) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", loginDTO.getUsernameOrPhone())
                .or()
                .eq("phone_number", loginDTO.getUsernameOrPhone());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查账号是否被禁用
        if ("banned".equals(user.getAccountStatus())) {
            throw new RuntimeException("账号已被禁用，请联系客服");
        }

        return user;
    }

    @Override
    public User register(RegisterDTO registerDTO) {
        // 检查用户名是否已存在
        QueryWrapper<User> usernameWrapper = new QueryWrapper<>();
        usernameWrapper.eq("username", registerDTO.getUsername());
        if (userMapper.selectOne(usernameWrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查手机号是否已存在
        QueryWrapper<User> phoneWrapper = new QueryWrapper<>();
        phoneWrapper.eq("phone_number", registerDTO.getPhoneNumber());
        if (userMapper.selectOne(phoneWrapper) != null) {
            throw new RuntimeException("手机号已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(registerDTO.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerDTO.getPassword()));
        user.setPhoneNumber(registerDTO.getPhoneNumber());
        user.setAccountLevel(1);
        user.setAccountStatus("active");
        user.setPostCount(0);
        user.setFollowingCount(0);
        user.setFollowerCount(0);

        userMapper.insert(user);
        return user;
    }

    @Override
    public User getUserById(String userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User updateAvatar(String userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setAvatarUrl(avatarUrl);
        userMapper.updateById(user);
        return user;
    }

    @Override
    public User getUserWithStats(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            updateUserStats(userId);
            user = userMapper.selectById(userId);
        }
        return user;
    }

    @Override
    public void updateUserStats(String userId) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            Long postCountLong = socialActivityMapper.selectCount(
                    new QueryWrapper<com.example.trendytoysocialecommercehd.entity.SocialActivity>()
                            .eq("user_id", userId)
            );
            int postCount = postCountLong != null ? postCountLong.intValue() : 0;
            int followingCount = followRelationshipMapper.countFollowing(userId);
            int followerCount = followRelationshipMapper.countFollower(userId);

            // 统计未使用的优惠券数量
            Long couponCountLong = userCouponMapper.selectCount(
                    new QueryWrapper<com.example.trendytoysocialecommercehd.entity.UserCoupon>()
                            .eq("user_id", userId)
                            .eq("status", "unused")
            );
            int couponCount = couponCountLong != null ? couponCountLong.intValue() : 0;

            // 统计盒柜中的藏品数量
            int cabinetCount = cabinetItemMapper.countByUserId(userId);

            // 统计收藏商品数量（从user_interaction表）
            Long favoriteCountLong = userInteractionMapper.selectCount(
                    new QueryWrapper<com.example.trendytoysocialecommercehd.entity.UserInteraction>()
                            .eq("user_id", userId)
                            .eq("action_type", "FAVORITE")
                            .eq("status", "ACTIVE")
            );
            int favoriteProductCount = favoriteCountLong != null ? favoriteCountLong.intValue() : 0;

            user.setPostCount(postCount);
            user.setFollowingCount(followingCount);
            user.setFollowerCount(followerCount);
            user.setCouponCount(couponCount);
            user.setCabinetCount(cabinetCount);
            user.setFavoriteProductCount(favoriteProductCount);
            userMapper.updateById(user);
        }
    }

    @Override
    public Page<User> getUserList(int page, int size, String accountStatus, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        QueryWrapper<User> wrapper = new QueryWrapper<>();

        if (StringUtils.hasText(accountStatus)) {
            wrapper.eq("account_status", accountStatus);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like("username", keyword)
                    .or()
                    .like("phone_number", keyword)
                    .or()
                    .like("email", keyword)
            );
        }

        wrapper.orderByDesc("register_time");
        return userMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public User updateUser(String userId, User user) {
        User existingUser = userMapper.selectById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 只更新允许修改的字段
        if (user.getUsername() != null) existingUser.setUsername(user.getUsername());
        if (user.getPhoneNumber() != null) existingUser.setPhoneNumber(user.getPhoneNumber());
        if (user.getEmail() != null) existingUser.setEmail(user.getEmail());
        if (user.getGender() != null) existingUser.setGender(user.getGender());
        if (user.getBirthDate() != null) existingUser.setBirthDate(user.getBirthDate());
        if (user.getLocation() != null) existingUser.setLocation(user.getLocation());
        if (user.getBio() != null) existingUser.setBio(user.getBio());
        if (user.getAccountStatus() != null) existingUser.setAccountStatus(user.getAccountStatus());
        if (user.getAccountLevel() != null) existingUser.setAccountLevel(user.getAccountLevel());
        if (user.getMembershipType() != null) existingUser.setMembershipType(user.getMembershipType());

        userMapper.updateById(existingUser);
        return existingUser;
    }

    @Override
    public void deleteUser(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        userMapper.deleteById(userId);
    }

    @Override
    public User updateUserStatus(String userId, String accountStatus) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setAccountStatus(accountStatus);
        userMapper.updateById(user);
        return user;
    }

}
