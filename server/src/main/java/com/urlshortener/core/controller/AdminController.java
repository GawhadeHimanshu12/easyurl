package com.urlshortener.core.controller;

import com.urlshortener.core.dto.UrlStatsResponseDto;
import com.urlshortener.core.entity.UserEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;

    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;

    @GetMapping("/users")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/urls")
    public ResponseEntity<List<UrlStatsResponseDto>> getAllUrls() {
        List<UrlStatsResponseDto> dtoList = urlRepository.findAll().stream()
                .map(entity -> UrlStatsResponseDto.builder()
                        .originalUrl(entity.getOriginalUrl())
                        .shortUrl(baseUrl + entity.getShortKey())
                        .clickCount(entity.getClickCount())
                        .createdAt(entity.getCreatedAt())
                        .expiresAt(entity.getExpiresAt())
                        // THE FIX: Populate full user info
                        .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                        .userName(entity.getUser() != null ? entity.getUser().getName() : null)
                        .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }
}