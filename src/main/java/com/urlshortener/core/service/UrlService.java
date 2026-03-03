package com.urlshortener.core.service;

import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.dto.UrlStatsResponseDto;
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

        UrlEntity urlEntity = UrlEntity.builder()
                .originalUrl(request.getOriginalUrl())
                .shortKey(shortKey)
                .expiresAt(expiryDate)
                .build();

        UrlEntity savedEntity = urlRepository.save(urlEntity);
        redisTemplate.opsForValue().set(shortKey, savedEntity.getOriginalUrl(), Duration.ofDays(1));

        return ShortenResponseDto.builder()
                .shortUrl(baseUrl + savedEntity.getShortKey())
                .expiresAt(savedEntity.getExpiresAt())
                .build();
    }

    @Transactional(readOnly = true)
    public String getOriginalUrl(String shortKey) {
        String cachedUrl = redisTemplate.opsForValue().get(shortKey);
        if (cachedUrl != null) {
            return cachedUrl;
        }

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
        log.debug("Incrementing click count for shortKey: {}", shortKey);
        urlRepository.incrementClickCount(shortKey);
    }

    @Transactional(readOnly = true)
    public UrlStatsResponseDto getUrlStats(String shortKey) {
        UrlEntity urlEntity = urlRepository.findByShortKey(shortKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        return UrlStatsResponseDto.builder()
                .originalUrl(urlEntity.getOriginalUrl())
                .shortUrl(baseUrl + urlEntity.getShortKey())
                .clickCount(urlEntity.getClickCount())
                .createdAt(urlEntity.getCreatedAt())
                .expiresAt(urlEntity.getExpiresAt())
                .build();
    }
}