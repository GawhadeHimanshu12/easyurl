package com.urlshortener.core.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.dto.UrlStatsResponseDto;
import com.urlshortener.core.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
public class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UrlService urlService;

    @Test
    void shouldCreateShortUrlSuccessfully() throws Exception {
        ShortenRequestDto request = new ShortenRequestDto();
        request.setOriginalUrl("https://www.google.com");

        ShortenResponseDto response = ShortenResponseDto.builder()
                .shortUrl("http://localhost:8080/abc123X")
                .expiresAt(LocalDateTime.now().plusMonths(6))
                .build();

        when(urlService.shortenUrl(any(ShortenRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Expect HTTP 201
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc123X"));
    }

    @Test
    void shouldReturnBadRequestWhenUrlIsInvalid() throws Exception {
        ShortenRequestDto request = new ShortenRequestDto();
        request.setOriginalUrl("invalid-url");

        mockMvc.perform(post("/api/v1/urls/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()) // Expect HTTP 400
                .andExpect(jsonPath("$.message").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.validationErrors.originalUrl").exists());
    }

    @Test
    void shouldReturnUrlStatsSuccessfully() throws Exception {
        UrlStatsResponseDto stats = UrlStatsResponseDto.builder()
                .originalUrl("https://www.apple.com")
                .shortUrl("http://localhost:8080/apple")
                .clickCount(5L)
                .build();

        when(urlService.getUrlStats(anyString())).thenReturn(stats);

        mockMvc.perform(get("/api/v1/urls/apple/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Expect HTTP 200
                .andExpect(jsonPath("$.clickCount").value(5));
    }
}