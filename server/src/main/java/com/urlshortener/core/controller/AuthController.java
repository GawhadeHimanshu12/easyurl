package com.urlshortener.core.controller;

import com.urlshortener.core.entity.UserEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.repository.UserRepository;
import com.urlshortener.core.security.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @GetMapping("/me")
    public ResponseEntity<UserEntity> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return ResponseEntity.ok(userRepository.findById(userDetails.getId()).orElseThrow());
    }

    @PostMapping("/claim")
    @Transactional
    public ResponseEntity<Void> claimAnonymousUrls(@RequestHeader(value = "X-Anonymous-Session", required = false) String anonId) {
        if (anonId != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                UserEntity user = userRepository.findById(userDetails.getId()).orElse(null);
                if (user != null) {
                    urlRepository.claimAnonymousUrls(user, anonId);
                }
            }
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}