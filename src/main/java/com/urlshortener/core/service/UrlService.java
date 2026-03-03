package com.urlshortener.core.service;

import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.entity.UrlEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.util.Base62Encoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;
    private final Base62Encoder base62Encoder;

    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;

    @Transactional
    public ShortenResponseDto shortenUrl(ShortenRequestDto request) {
        log.info("Received request to shorten URL: {}", request.getOriginalUrl());

        String shortKey;
        boolean isUnique = false;

        do {
            shortKey = base62Encoder.generateShortKey(7);
            if (urlRepository.findByShortKey(shortKey).isEmpty()) {
                isUnique = true;
            } else {
                log.warn("Collision detected for key: {}. Regenerating...", shortKey);
            }
        } while (!isUnique);

        UrlEntity urlEntity = UrlEntity.builder()
                .originalUrl(request.getOriginalUrl())
                .shortKey(shortKey)
                .expiresAt(LocalDateTime.now().plusMonths(6))
                .build();

        UrlEntity savedEntity = urlRepository.save(urlEntity);
        log.info("Successfully saved shortened URL with key: {}", shortKey);

        return ShortenResponseDto.builder()
                .shortUrl(baseUrl + savedEntity.getShortKey())
                .expiresAt(savedEntity.getExpiresAt())
                .build();
    }
}