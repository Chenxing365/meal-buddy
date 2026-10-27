package com.distributed.base.service;

import com.distributed.base.dto.LoginRequest;
import com.distributed.base.dto.LoginResponse;
import com.distributed.base.dto.RegisterRequest;
import com.distributed.base.dto.UserPreferenceDto;
import com.distributed.base.dto.UserResponse;
import com.distributed.base.entity.User;
import com.distributed.base.entity.UserPreference;
import com.distributed.base.exception.BusinessException;
import com.distributed.base.repository.UserPreferenceRepository;
import com.distributed.base.repository.UserRepository;
import com.distributed.base.service.impl.UserServiceImpl;
import com.distributed.base.util.AesUtil;
import com.distributed.base.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("M1阶段-UserService核心业务流转单元测试")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private PasswordEncoder passwordEncoder;
    private AesUtil aesUtil;
    private JwtUtil jwtUtil;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        aesUtil = new AesUtil("MealBuddyAesKey!");
        jwtUtil = new JwtUtil("MealBuddySecretKeyForJwtAuthenticationTokenSigning20261027", 7200000L, 604800000L);

        userService = new UserServiceImpl(
                userRepository,
                userPreferenceRepository,
                passwordEncoder,
                aesUtil,
                jwtUtil,
                redisTemplate
        );

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("测试用户注册：密码BCrypt散列、手机号AES加密存储、初始化默认偏好")
    void testRegisterSuccess() {
        RegisterRequest req = RegisterRequest.builder()
                .username("jens_test")
                .password("Password123")
                .phone("13812345678")
                .nickname("Jens")
                .build();

        when(userRepository.existsByUsername("jens_test")).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("u_test_id");
            u.setCreatedAt(LocalDateTime.now());
            return u;
        });

        UserResponse response = userService.register(req);

        assertNotNull(response);
        assertEquals("jens_test", response.getUsername());
        assertEquals("138****5678", response.getPhone());

        // 验证密码为 BCrypt 散列且手机号为 AES 密文
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertTrue(savedUser.getPassword().startsWith("$2a$") || savedUser.getPassword().startsWith("$2b$"));
        assertTrue(passwordEncoder.matches("Password123", savedUser.getPassword()));
        assertNotEquals("13812345678", savedUser.getPhone());
        assertEquals("13812345678", aesUtil.decrypt(savedUser.getPhone()));

        // 验证初始化 UserPreference
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("测试用户注册：同名注册被拦截抛出400异常")
    void testRegisterDuplicateUsername() {
        RegisterRequest req = RegisterRequest.builder()
                .username("existing_user")
                .password("123456")
                .phone("13800000000")
                .build();

        when(userRepository.existsByUsername("existing_user")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(req));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("用户名已存在"));
    }

    @Test
    @DisplayName("测试用户登录：验证密码成功返回双Token")
    void testLoginSuccess() {
        String rawPassword = "SecretPassword";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        String encryptedPhone = aesUtil.encrypt("13987654321");

        User mockUser = User.builder()
                .id("u_login_id")
                .username("jens_login")
                .password(encodedPassword)
                .phone(encryptedPhone)
                .role(0)
                .status(1)
                .build();

        when(userRepository.findByUsername("jens_login")).thenReturn(Optional.of(mockUser));

        LoginRequest req = LoginRequest.builder()
                .username("jens_login")
                .password(rawPassword)
                .build();

        LoginResponse resp = userService.login(req);

        assertNotNull(resp);
        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertEquals("139****4321", resp.getUser().getPhone());
    }

    @Test
    @DisplayName("测试用户登录：密码错误抛出400异常")
    void testLoginWrongPassword() {
        User mockUser = User.builder()
                .id("u_login_id")
                .username("jens_login")
                .password(passwordEncoder.encode("correct_pass"))
                .status(1)
                .build();

        when(userRepository.findByUsername("jens_login")).thenReturn(Optional.of(mockUser));

        LoginRequest req = LoginRequest.builder()
                .username("jens_login")
                .password("wrong_pass")
                .build();

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(req));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("用户名或密码错误"));
    }

    @Test
    @DisplayName("测试偏好更新：切换为减肥模式(slim)并设置500大卡热量")
    void testUpdatePreferences() {
        String userId = "u_pref_id";
        UserPreference existing = UserPreference.builder()
                .userId(userId)
                .recommendMode("normal")
                .calorieLimit(800)
                .build();

        when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        UserPreferenceDto updateReq = UserPreferenceDto.builder()
                .recommendMode("slim")
                .calorieLimit(500)
                .flavorTags(List.of("清淡", "少油"))
                .allergyTags(List.of("花生"))
                .build();

        UserPreferenceDto result = userService.updatePreferences(userId, updateReq);

        assertEquals("slim", result.getRecommendMode());
        assertEquals(500, result.getCalorieLimit());
        assertEquals(List.of("清淡", "少油"), result.getFlavorTags());
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }
}
