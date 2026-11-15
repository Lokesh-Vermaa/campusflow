package com.jklu.campusflow.auth;

import com.jklu.campusflow.auth.dto.LoginRequest;
import com.jklu.campusflow.auth.dto.UserProfileResponse;
import com.jklu.campusflow.common.error.RateLimitExceededException;
import com.jklu.campusflow.common.error.ResourceNotFoundException;
import com.jklu.campusflow.common.security.JwtService;
import com.jklu.campusflow.common.security.LoginRateLimiter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginRateLimiter loginRateLimiter
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginRequest request, String clientIp) {
        if (loginRateLimiter.isBlocked(clientIp)) {
            long retryAfter = loginRateLimiter.getRetryAfterSeconds(clientIp);
            throw new RateLimitExceededException(
                    "Too many failed login attempts. Please wait " + retryAfter + " seconds before trying again.",
                    retryAfter
            );
        }

        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginRateLimiter.recordFailure(clientIp);
            throw new BadCredentialsException("Invalid email or password");
        }

        loginRateLimiter.recordSuccess(clientIp);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        UserProfileResponse profile = mapToProfile(user);

        return new LoginResult(profile, token);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return mapToProfile(user);
    }

    private UserProfileResponse mapToProfile(User user) {
        return new UserProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getDepartment(),
                null
        );
    }

    public record LoginResult(UserProfileResponse profile, String token) {}
}
