package com.urlshortener.core.service;

import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.entity.UrlEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.util.Base62Encoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private Base62Encoder base62Encoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080/");
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldSuccessfullyShortenUrlWithCustomAlias() {
        ShortenRequestDto request = new ShortenRequestDto();
        request.setOriginalUrl("https://google.com");
        request.setCustomAlias("my-custom-link");

        when(urlRepository.findByShortKey("my-custom-link")).thenReturn(Optional.empty());

        UrlEntity savedEntity = new UrlEntity(1L, "https://google.com", "my-custom-link", LocalDateTime.now(), LocalDateTime.now().plusMonths(6));
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);

        ShortenResponseDto response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/my-custom-link", response.getShortUrl());
        verify(base62Encoder, never()).generateShortKey(anyInt());
    }

    @Test
    void shouldThrowConflictExceptionWhenCustomAliasExists() {
        ShortenRequestDto request = new ShortenRequestDto();
        request.setOriginalUrl("https://google.com");
        request.setCustomAlias("taken-link");

        UrlEntity existingEntity = new UrlEntity();
        when(urlRepository.findByShortKey("taken-link")).thenReturn(Optional.of(existingEntity));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.shortenUrl(request);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(urlRepository, never()).save(any(UrlEntity.class));
    }

    @Test
    void shouldReturnUrlFromCacheWhenAvailable() {
        when(valueOperations.get("git123")).thenReturn("https://github.com");
        String result = urlService.getOriginalUrl("git123");
        assertEquals("https://github.com", result);
        verify(urlRepository, never()).findByShortKey(anyString());
    }
}