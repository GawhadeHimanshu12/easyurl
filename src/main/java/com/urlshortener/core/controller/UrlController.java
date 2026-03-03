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

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponseDto> shortenUrl(@Valid @RequestBody ShortenRequestDto request) {
        ShortenResponseDto response = urlService.shortenUrl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{shortKey}/stats")
    public ResponseEntity<UrlStatsResponseDto> getUrlStats(@PathVariable String shortKey) {
        UrlStatsResponseDto stats = urlService.getUrlStats(shortKey);
        return ResponseEntity.ok(stats);
    }
}