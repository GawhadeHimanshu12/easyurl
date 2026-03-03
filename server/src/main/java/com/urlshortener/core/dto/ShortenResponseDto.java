package com.urlshortener.core.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShortenResponseDto {
    private String shortUrl;
    private LocalDateTime expiresAt;
}