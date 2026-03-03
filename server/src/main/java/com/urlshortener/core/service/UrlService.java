package com.urlshortener.core.service;

import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.dto.UrlStatsResponseDto;
import com.urlshortener.core.entity.UrlEntity;
import com.urlshortener.core.entity.UserEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.repository.UserRepository;
import com.urlshortener.core.security.CustomUserDetails;
import com.urlshortener.core.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;

    @Transactional
    public ShortenResponseDto shortenUrl(ShortenRequestDto request, String anonId) {
        String shortKey;

        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            shortKey = request.getCustomAlias().trim();
            if (urlRepository.findByShortKey(shortKey).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Custom alias already exists");
            }
        } else {
            boolean isUnique = false;
            do {
                shortKey = base62Encoder.generateShortKey(7);
                if (urlRepository.findByShortKey(shortKey).isEmpty()) {
                    isUnique = true;
                }
            } while (!isUnique);
        }

        LocalDateTime expiryDate = request.getExpiresAt() != null
                ? request.getExpiresAt()
                : LocalDateTime.now().plusMonths(6);

        UrlEntity.UrlEntityBuilder builder = UrlEntity.builder()
                .originalUrl(request.getOriginalUrl())
                .shortKey(shortKey)
                .expiresAt(expiryDate);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            UserEntity user = userRepository.findById(userDetails.getId()).orElse(null);
            builder.user(user);
        } else if (anonId != null) {
            builder.anonymousSessionId(anonId);
        }

        UrlEntity savedEntity = urlRepository.save(builder.build());
        redisTemplate.opsForValue().set(shortKey, savedEntity.getOriginalUrl(), Duration.ofDays(1));

        return ShortenResponseDto.builder()
                .shortUrl(baseUrl + savedEntity.getShortKey())
                .expiresAt(savedEntity.getExpiresAt())
                .build();
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortKey) {
        String cachedUrl = redisTemplate.opsForValue().get(shortKey);
        if (cachedUrl != null) { return cachedUrl; }

        UrlEntity urlEntity = urlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        if (urlEntity.getExpiresAt() != null && urlEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            redisTemplate.delete(shortKey);
            throw new ResponseStatusException(HttpStatus.GONE, "URL has expired");
        }

        redisTemplate.opsForValue().set(shortKey, urlEntity.getOriginalUrl(), Duration.ofDays(1));
        return urlEntity.getOriginalUrl();
    }

    @Transactional
    public void incrementClickCount(String shortKey) {
        urlRepository.incrementClickCount(shortKey);
    }

    @Transactional(readOnly = true)
    public UrlStatsResponseDto getUrlStats(String shortKey) {
        UrlEntity urlEntity = urlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));
        return mapToStatsDto(urlEntity);
    }

    @Transactional(readOnly = true)
    public List<UrlStatsResponseDto> getMyUrls() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        UserEntity user = userRepository.findById(userDetails.getId()).orElseThrow();
        return urlRepository.findByUser(user).stream()
                .map(this::mapToStatsDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUrl(String shortKey) {
        UrlEntity url = urlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        // Security check: Ensure the logged-in user owns this URL
        if (url.getUser() == null || !url.getUser().getId().equals(userDetails.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this URL");
        }

        redisTemplate.delete(shortKey);
        urlRepository.delete(url);
    }

    private UrlStatsResponseDto mapToStatsDto(UrlEntity entity) {
        return UrlStatsResponseDto.builder()
                .originalUrl(entity.getOriginalUrl())
                .shortUrl(baseUrl + entity.getShortKey())
                .clickCount(entity.getClickCount())
                .createdAt(entity.getCreatedAt())
                .expiresAt(entity.getExpiresAt())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userName(entity.getUser() != null ? entity.getUser().getName() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .build();
    }
}