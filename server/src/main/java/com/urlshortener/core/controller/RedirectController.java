package com.urlshortener.core.controller;

import com.urlshortener.core.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final UrlService urlService;

    private static final Set<String> RESERVED_WORDS = Set.of("login", "oauth2", "error", "api", "favicon.ico", "dashboard", "admin", "assets", "index.html");

    @GetMapping("/{shortKey:[a-zA-Z0-9-_]+}")
    public ResponseEntity<Void> redirectToOriginal(@PathVariable String shortKey) {

        if (RESERVED_WORDS.contains(shortKey)) {
            return ResponseEntity.notFound().build();
        }

        // Get the original URL
        String originalUrl = urlService.getOriginalUrl(shortKey);

        // Increment the analytics counter
        urlService.incrementClickCount(shortKey);

        // Perform the redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }
}