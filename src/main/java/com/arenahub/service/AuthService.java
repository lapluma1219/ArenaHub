package com.arenahub.service;

import com.arenahub.dto.AuthDtos.AuthResponse;
import com.arenahub.dto.AuthDtos.LoginRequest;
import com.arenahub.dto.AuthDtos.RegisterRequest;
import com.arenahub.dto.PlayerResponse;
import com.arenahub.entity.PlayerProfile;
import com.arenahub.entity.UserAccount;
import com.arenahub.exception.BusinessException;
import com.arenahub.repository.UserAccountRepository;
import com.arenahub.security.JwtService;
import com.arenahub.security.PasswordHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;
    private final LeaderboardCacheService leaderboardCacheService;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordHasher passwordHasher,
            JwtService jwtService,
            LeaderboardCacheService leaderboardCacheService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.jwtService = jwtService;
        this.leaderboardCacheService = leaderboardCacheService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setPasswordHash(passwordHasher.hash(request.password()));

        PlayerProfile profile = new PlayerProfile();
        profile.setUser(user);
        profile.setNickname(request.nickname());
        user.setProfile(profile);

        UserAccount saved = userRepository.save(user);
        leaderboardCacheService.evictAll();
        String token = jwtService.createToken(saved.getId(), saved.getUsername(), saved.getProfile().getId());
        return new AuthResponse(token, PlayerResponse.from(saved.getProfile()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误"));
        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtService.createToken(user.getId(), user.getUsername(), user.getProfile().getId());
        return new AuthResponse(token, PlayerResponse.from(user.getProfile()));
    }
}
