package com.urlshortener.core.controller;

import com.urlshortener.core.dto.ShortenRequestDto;
import com.urlshortener.core.dto.ShortenResponseDto;
import com.urlshortener.core.dto.UrlStatsResponseDto;
import com.urlshortener.core.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponseDto> shortenUrl(
            @Valid @RequestBody ShortenRequestDto request,
            // Header for programmatic API access, Cookie for browser pre-login flow
            @RequestHeader(value = "X-Anonymous-Session", required = false) String headerAnonId,
            @CookieValue(value = "anon_id", required = false) String cookieAnonId) {

        String anonId = cookieAnonId != null ? cookieAnonId : headerAnonId;
        ShortenResponseDto response = urlService.shortenUrl(request, anonId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortKey}/stats")
    public ResponseEntity<UrlStatsResponseDto> getUrlStats(@PathVariable String shortKey) {
        return ResponseEntity.ok(urlService.getUrlStats(shortKey));
    }

    @GetMapping("/my-urls")
    public ResponseEntity<List<UrlStatsResponseDto>> getMyUrls() {
        return ResponseEntity.ok(urlService.getMyUrls());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(@PathVariable Long id) {
        urlService.deleteUrl(id);
        return ResponseEntity.noContent().build();
    }
}