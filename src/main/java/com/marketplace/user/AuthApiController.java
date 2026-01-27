package com.marketplace.user;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApiController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager; // Need to expose this bean

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPasswordHash(request.getPassword());
            user.setRole(Role.BUYER);

            User registeredUser = userService.registerUser(user);

            return ResponseEntity.ok(new AuthResponse(registeredUser.getId(), registeredUser.getUsername(),
                    "User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Create session manually for the API response to be useful in subsequent calls
            // if cookie-based
            HttpSession session = servletRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());

            User user = userService.findByUsername(request.getUsername()).orElseThrow();
            return ResponseEntity.ok(new AuthResponse(user.getId(), user.getUsername(), "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new ErrorResponse("Invalid credentials"));
        }
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class AuthResponse {
        private Long userId;
        private String username;
        private String message;
    }

    @Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
    }
}
