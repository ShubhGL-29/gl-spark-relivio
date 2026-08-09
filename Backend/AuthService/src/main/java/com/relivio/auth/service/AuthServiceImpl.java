package com.relivio.auth.service;

import com.relivio.auth.dto.AuthResponse;
import com.relivio.auth.dto.LoginRequest;
import com.relivio.auth.dto.RegisterRequest;
import com.relivio.auth.dto.UserResponse;
import com.relivio.auth.entity.Session;
import com.relivio.auth.entity.User;
import com.relivio.auth.enums.Role;
import com.relivio.auth.exception.InvalidCredentialsException;
import com.relivio.auth.exception.ResourceNotFoundException;
import com.relivio.auth.repository.SessionRepository;
import com.relivio.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final long SESSION_HOURS = 24L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        User user = createAccount(request, Role.CITIZEN);
        log.info("Registered new citizen account id={} phone={}", user.getId(), user.getPhone());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse registerVolunteer(RegisterRequest request) {
        User user = createAccount(request, Role.VOLUNTEER);
        log.info("Registered new volunteer account id={} phone={}", user.getId(), user.getPhone());
        return buildAuthResponse(user);
    }

    private User createAccount(RegisterRequest request, Role role) {
        String phone = request.getPhone().trim();
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("An account with this phone number already exists.");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank() && userRepository.existsByEmail(request.getEmail().trim())) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setPhone(phone);
        user.setEmail(request.getEmail() == null ? null : request.getEmail().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByPhone(request.getPhone().trim())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid phone number or password."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid phone number or password.");
        }
        log.info("Login successful for user id={} role={}", user.getId(), user.getRole());
        return buildAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getByIdentity(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone: " + phone));
    }

    @Override
    @Transactional
    public void logout(String token) {
        sessionRepository.deleteByToken(token);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = generateToken();
        Session session = new Session();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusHours(SESSION_HOURS));
        sessionRepository.save(session);

        return AuthResponse.builder()
                .token(token)
                .user(toResponse(user))
                .build();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
