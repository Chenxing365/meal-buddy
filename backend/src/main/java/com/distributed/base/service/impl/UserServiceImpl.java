package com.distributed.base.service.impl;

import com.distributed.base.dto.*;
import com.distributed.base.entity.User;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.repository.UserRepository;
import com.distributed.base.service.UserService;
import com.distributed.base.util.AesUtil;
import com.distributed.base.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * 用户核心领域服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AesUtil aesUtil;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PREF_CACHE_PREFIX = "user:preference:";
    private static final String FAVORITE_CACHE_PREFIX = "user:favorites:";

    @Override
    @Transactional
    public UserResponse register(RegisterRequest req) {
        log.info("[UserService] 开始注册用户: username={}", req.getUsername());

        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException(400, "用户名已存在，请更换");
        }

        String encryptedPhone = aesUtil.encrypt(req.getPhone());
        if (userRepository.existsByPhone(encryptedPhone)) {
            throw new BusinessException(400, "该手机号已被注册，请直接登录");
        }

        String encodedPassword = passwordEncoder.encode(req.getPassword());

        User user = User.builder()
                .username(req.getUsername())
                .password(encodedPassword)
                .nickname(StringUtils.hasText(req.getNickname()) ? req.getNickname() : req.getUsername())
                .phone(encryptedPhone)
                .role(0)
                .status(1)
                .build();

        User savedUser = userRepository.save(user);

        // 初始化默认饮食偏好
        UserPreference preference = UserPreference.builder()
                .userId(savedUser.getId())
                .recommendMode("normal")
                .flavorTags(new ArrayList<>())
                .calorieLimit(800)
                .allergyTags(new ArrayList<>())
                .build();
        userPreferenceRepository.save(preference);

        // 写入 Redis 偏好缓存 7 天
        cachePreference(savedUser.getId(), toDto(preference));

        log.info("[UserService] 用户注册成功: userId={}", savedUser.getId());
        return toUserResponse(savedUser, req.getPhone());
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        log.info("[UserService] 用户尝试登录: username={}", req.getUsername());

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new BusinessException(400, "用户名或密码错误"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系平台管理员");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        String plainPhone = aesUtil.decrypt(user.getPhone());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(toUserResponse(user, plainPhone))
                .build();
    }

    @Override
    public UserResponse getUserInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        String plainPhone = aesUtil.decrypt(user.getPhone());
        return toUserResponse(user, plainPhone);
    }

    @Override
    public UserResponse updateUserInfo(String userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (StringUtils.hasText(req.getNickname())) {
            user.setNickname(req.getNickname());
        }
        if (StringUtils.hasText(req.getAvatar())) {
            user.setAvatar(req.getAvatar());
        }

        User updated = userRepository.save(user);
        String plainPhone = aesUtil.decrypt(updated.getPhone());
        return toUserResponse(updated, plainPhone);
    }

    @Override
    public UserPreferenceDto getPreferences(String userId) {
        String cacheKey = PREF_CACHE_PREFIX + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof UserPreferenceDto dto) {
                return dto;
            }
        } catch (Exception e) {
            log.warn("[UserService] 读取偏好缓存异常: {}", e.getMessage());
        }

        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPreference init = UserPreference.builder()
                            .userId(userId)
                            .recommendMode("normal")
                            .calorieLimit(800)
                            .build();
                    return userPreferenceRepository.save(init);
                });

        UserPreferenceDto dto = toDto(preference);
        cachePreference(userId, dto);
        return dto;
    }

    @Override
    public UserPreferenceDto updatePreferences(String userId, UserPreferenceDto req) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> UserPreference.builder().userId(userId).build());

        if (StringUtils.hasText(req.getRecommendMode())) {
            preference.setRecommendMode(req.getRecommendMode());
        }
        if (req.getFlavorTags() != null) {
            preference.setFlavorTags(req.getFlavorTags());
        }
        if (req.getCalorieLimit() != null) {
            preference.setCalorieLimit(req.getCalorieLimit());
        }
        if (req.getAllergyTags() != null) {
            preference.setAllergyTags(req.getAllergyTags());
        }
        preference.setUpdatedAt(LocalDateTime.now());

        UserPreference saved = userPreferenceRepository.save(preference);
        UserPreferenceDto dto = toDto(saved);

        // 刷新 Redis 缓存
        cachePreference(userId, dto);
        return dto;
    }

    @Override
    public boolean toggleFavorite(String userId, String type, String targetId) {
        if (!StringUtils.hasText(type) || !StringUtils.hasText(targetId)) {
            throw new BusinessException(400, "参数错误：收藏类型与目标ID不能为空");
        }
        String cacheKey = FAVORITE_CACHE_PREFIX + userId + ":" + type;
        Boolean isMember = redisTemplate.opsForSet().isMember(cacheKey, targetId);
        if (Boolean.TRUE.equals(isMember)) {
            redisTemplate.opsForSet().remove(cacheKey, targetId);
            return false;
        } else {
            redisTemplate.opsForSet().add(cacheKey, targetId);
            return true;
        }
    }

    @Override
    public Set<String> getFavorites(String userId, String type, int page, int size) {
        String cacheKey = FAVORITE_CACHE_PREFIX + userId + ":" + type;
        Set<Object> members = redisTemplate.opsForSet().members(cacheKey);
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new java.util.LinkedHashSet<>();
        for (Object item : members) {
            if (item != null) {
                result.add(item.toString());
            }
        }
        return result;
    }

    @Override
    public Page<UserResponse> listUsersForAdmin(Integer role, Integer status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage;

        if (role != null && status != null) {
            userPage = userRepository.findByRoleAndStatus(role, status, pageRequest);
        } else if (role != null) {
            userPage = userRepository.findByRole(role, pageRequest);
        } else if (status != null) {
            userPage = userRepository.findByStatus(status, pageRequest);
        } else {
            userPage = userRepository.findAll(pageRequest);
        }

        return userPage.map(u -> {
            String plainPhone = "";
            try {
                plainPhone = aesUtil.decrypt(u.getPhone());
            } catch (Exception ignored) {
            }
            return toUserResponse(u, plainPhone);
        });
    }

    @Override
    public void updateUserStatus(String userId, Integer status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "目标用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }

    private void cachePreference(String userId, UserPreferenceDto dto) {
        try {
            String cacheKey = PREF_CACHE_PREFIX + userId;
            redisTemplate.opsForValue().set(cacheKey, dto, Duration.ofDays(7));
        } catch (Exception e) {
            log.warn("[UserService] 写入偏好缓存失败: {}", e.getMessage());
        }
    }

    private UserResponse toUserResponse(User user, String plainPhone) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(AesUtil.maskPhone(plainPhone))
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserPreferenceDto toDto(UserPreference p) {
        return UserPreferenceDto.builder()
                .recommendMode(p.getRecommendMode())
                .flavorTags(p.getFlavorTags())
                .calorieLimit(p.getCalorieLimit())
                .allergyTags(p.getAllergyTags())
                .build();
    }
}
