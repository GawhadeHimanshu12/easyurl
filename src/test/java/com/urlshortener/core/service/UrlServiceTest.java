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
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

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

    @InjectMocks
    private UrlService urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080/");
    }

    @Test
    void shouldSuccessfullyShortenUrl() {
        ShortenRequestDto request = new ShortenRequestDto();
        request.setOriginalUrl("https://google.com");

        when(base62Encoder.generateShortKey(7)).thenReturn("abc123X");
        when(urlRepository.findByShortKey("abc123X")).thenReturn(Optional.empty());

        UrlEntity savedEntity = new UrlEntity(1L, "https://google.com", "abc123X", LocalDateTime.now(), LocalDateTime.now().plusMonths(6));
        when(urlRepository.save(any(UrlEntity.class))).thenReturn(savedEntity);

        ShortenResponseDto response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("http://localhost:8080/abc123X", response.getShortUrl());
    }

    @Test
    void shouldReturnOriginalUrlWhenValidShortKeyProvided() {
        UrlEntity entity = new UrlEntity(1L, "https://github.com", "git123", LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        when(urlRepository.findByShortKey("git123")).thenReturn(Optional.of(entity));

        String result = urlService.getOriginalUrl("git123");

        assertEquals("https://github.com", result);
    }

    @Test
    void shouldThrowNotFoundExceptionWhenShortKeyDoesNotExist() {
        when(urlRepository.findByShortKey("invalid")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.getOriginalUrl("invalid");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowGoneExceptionWhenUrlIsExpired() {
        UrlEntity expiredEntity = new UrlEntity(1L, "https://expired.com", "exp123", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1));
        when(urlRepository.findByShortKey("exp123")).thenReturn(Optional.of(expiredEntity));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.getOriginalUrl("exp123");
        });

        assertEquals(HttpStatus.GONE, exception.getStatusCode());
    }
}