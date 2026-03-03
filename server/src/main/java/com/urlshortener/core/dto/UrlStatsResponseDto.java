package com.urlshortener.core.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UrlStatsResponseDto {
    private String originalUrl;
    private String shortUrl;
    private Long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private Long userId;
    private String userName;
    private String userEmail;
}