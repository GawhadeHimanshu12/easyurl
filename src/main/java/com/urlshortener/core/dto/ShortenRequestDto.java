package com.urlshortener.core.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShortenRequestDto {

    @NotBlank(message = "URL cannot be blank")
    @URL(message = "Invalid URL format. Must include http:// or https://")
    private String originalUrl;

    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Alias can only contain letters, numbers, hyphens, and underscores")
    private String customAlias;

    @Future(message = "Expiration date must be in the future")
    private LocalDateTime expiresAt;
}