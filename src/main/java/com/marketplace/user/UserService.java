package com.marketplace.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public java.util.List<User> getRecentUsers() {
        return userRepository.findTop5ByOrderByCreatedAtDesc();
    }

    public org.springframework.data.domain.Page<User> getUsers(String search,
            org.springframework.data.domain.Pageable pageable) {
        if (search != null && !search.trim().isEmpty()) {
            return userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search,
                    pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void updateUserStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(status);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    @Transactional
    public void updateProfile(Long userId, String username, String email, String phoneNumber, String address,
            String password, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if (username != null && !username.isBlank())
            user.setUsername(username);
        if (email != null && !email.isBlank())
            user.setEmail(email);
        if (phoneNumber != null)
            user.setPhoneNumber(phoneNumber);
        if (address != null)
            user.setAddress(address);

        if (newPassword != null && !newPassword.isBlank()) {
            if (password == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
                throw new RuntimeException("Invalid current password");
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }
        userRepository.save(user);
    }
}
