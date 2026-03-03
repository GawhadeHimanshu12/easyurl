package com.urlshortener.core.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.Data;

@Data
public class ShortenRequestDto {

    @NotBlank(message = "URL cannot be blank")
    @URL(message = "Invalid URL format. Must include http:// or https://")
    private String originalUrl;
}