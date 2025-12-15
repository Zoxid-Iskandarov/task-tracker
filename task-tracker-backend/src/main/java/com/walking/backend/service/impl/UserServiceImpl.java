package com.walking.backend.service.impl;

import com.walking.backend.domain.dto.auth.SignUpRequest;
import com.walking.backend.domain.dto.user.UserResponse;
import com.walking.backend.domain.exception.DuplicateException;
import com.walking.backend.domain.exception.ObjectNotFoundException;
import com.walking.backend.domain.model.User;
import com.walking.backend.repository.UserRepository;
import com.walking.backend.service.UserService;
import com.walking.backend.service.mapper.user.SignUpRequestMapper;
import com.walking.backend.service.mapper.user.UserResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;
    private final SignUpRequestMapper signUpRequestMapper;

    @Override
    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userResponseMapper::toDto)
                .orElseThrow(() -> new ObjectNotFoundException("User '%s' not found".formatted(username)));
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ObjectNotFoundException("User with id '%d' not found".formatted(userId)));
    }

    @Override
    @Transactional
    public UserResponse createUser(SignUpRequest signUpRequest) {
        if (userRepository.findByUsername(signUpRequest.username()).isPresent()) {
            throw new DuplicateException("This username '%s' is already taken".formatted(signUpRequest.username()));
        }

        if (userRepository.findByEmail(signUpRequest.email()).isPresent()) {
            throw new DuplicateException("This email '%s' is already taken".formatted(signUpRequest.email()));
        }

        return Optional.of(signUpRequest)
                .map(signUpRequestMapper::toEntity)
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
                }).map(userResponseMapper::toDto)
                .orElseThrow();
    }
}
