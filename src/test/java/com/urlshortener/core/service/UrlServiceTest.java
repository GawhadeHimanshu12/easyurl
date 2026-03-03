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
    void shouldSuccessfullyShortenUrlAndPreWarmCache() {
        ShortenRequestDto request = new ShortenRequestDto();
        request.setOriginalUrl("https://google.com");

        when(base62Encoder.generateShortKey(7)).thenReturn("abc123X");
        when(urlRepository.findByShortKey("abc123X")).thenReturn(Optional.empty());

        UrlEntity savedEntity = new UrlEntity(1L, "https://google.com", "abc123X", LocalDateTime.now(), LocalDateTime.now().plusMonths(6));
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);

        ShortenResponseDto response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/abc123X", response.getShortUrl());

        verify(valueOperations, times(1)).set(eq("abc123X"), eq("https://google.com"), any(Duration.class));
    }

    @Test
    void shouldReturnUrlFromCacheWhenAvailable() {
        when(valueOperations.get("git123")).thenReturn("https://github.com");

        String result = urlService.getOriginalUrl("git123");

        assertEquals("https://github.com", result);
        verify(urlRepository, never()).findByShortKey(anyString());
    }

    @Test
    void shouldReturnUrlFromDatabaseWhenCacheMissAndSaveToCache() {
        when(valueOperations.get("db123")).thenReturn(null); // Cache miss

                UrlEntity entity = new UrlEntity(1L, "https://database.com", "db123", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        when(urlRepository.findByShortKey("db123")).thenReturn(Optional.of(entity));

        String result = urlService.getOriginalUrl("db123");

        assertEquals("https://database.com", result);
        verify(urlRepository, times(1)).findByShortKey("db123");
        verify(valueOperations, times(1)).set(eq("db123"), eq("https://database.com"), any(Duration.class));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenShortKeyDoesNotExist() {
        when(valueOperations.get("invalid")).thenReturn(null);
        when(urlRepository.findByShortKey("invalid")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.getOriginalUrl("invalid");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}