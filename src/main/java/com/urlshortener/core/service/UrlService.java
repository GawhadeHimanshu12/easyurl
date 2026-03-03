package com.urlshortener.core.service;

import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.entity.UrlEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;

    @Transactional
    public ShortenResponseDto shortenUrl(ShortenRequestDto request) {
        log.info("Received request to shorten URL: {}", request.getOriginalUrl());

        String shortKey;

        // Handle Custom Alias vs Random Generation
        if (request.getCustomAlias() != null && !request.getCustomAlias().trim().isEmpty()) {
            shortKey = request.getCustomAlias().trim();
            log.info("Processing custom alias: {}", shortKey);

            // If the user wants an alias that is already taken, we must reject the request
            if (urlRepository.findByShortKey(shortKey).isPresent()) {
                log.warn("Custom alias '{}' already exists", shortKey);
                // HTTP 409 Conflict is the standard code for duplicate resource states
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Custom alias already exists");
            }
        } else {
            // Fallback to random Base62 generation
            boolean isUnique = false;
            do {
                shortKey = base62Encoder.generateShortKey(7);
                if (urlRepository.findByShortKey(shortKey).isEmpty()) {
                    isUnique = true;
                }
            } while (!isUnique);
        }

        // Handle Expiration Date
        LocalDateTime expiryDate = request.getExpiresAt() != null
                ? request.getExpiresAt()
                : LocalDateTime.now().plusMonths(6);

        // Save to Database
        UrlEntity urlEntity = UrlEntity.builder()
                .originalUrl(request.getOriginalUrl())
                .shortKey(shortKey)
                .expiresAt(expiryDate)
                .build();

        UrlEntity savedEntity = urlRepository.save(urlEntity);
        log.info("Successfully saved shortened URL with key: {}", shortKey);

        // Pre-warm the cache
        redisTemplate.opsForValue().set(shortKey, savedEntity.getOriginalUrl(), Duration.ofDays(1));

        return ShortenResponseDto.builder()
                .shortUrl(baseUrl + savedEntity.getShortKey())
                .expiresAt(savedEntity.getExpiresAt())
                .build();
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortKey) {
        log.info("Fetching original URL for shortKey: {}", shortKey);

        String cachedUrl = redisTemplate.opsForValue().get(shortKey);
        if (cachedUrl != null) {
            log.info("CACHE HIT: Found original URL in Redis for key: {}", shortKey);
            return cachedUrl;
        }

        log.info("CACHE MISS: Querying database for key: {}", shortKey);

        UrlEntity urlEntity = urlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> {
                    log.error("URL not found for shortKey: {}", shortKey);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
                });

        if (urlEntity.getExpiresAt() != null && urlEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Short URL {} has expired", shortKey);
            // Delete from cache if it was somehow stuck there, though it shouldn't be reached
            redisTemplate.delete(shortKey);
            throw new ResponseStatusException(HttpStatus.GONE, "URL has expired");
        }

        redisTemplate.opsForValue().set(shortKey, urlEntity.getOriginalUrl(), Duration.ofDays(1));
        log.info("Saved shortKey: {} to Redis Cache", shortKey);

        return urlEntity.getOriginalUrl();
    }
}