package com.urlshortener.core.controller;

import com.urlshortener.core.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
public class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Test
    void shouldRedirectToOriginalUrlSuccessfully() throws Exception {
        String shortKey = "git123";
        String originalUrl = "https://github.com";
        when(urlService.getOriginalUrl(shortKey)).thenReturn(originalUrl);

        doNothing().when(urlService).incrementClickCount(shortKey);

        mockMvc.perform(get("/" + shortKey))
                .andExpect(status().isFound()) // Expect HTTP 302
                .andExpect(header().string("Location", originalUrl));

        verify(urlService, times(1)).incrementClickCount(shortKey);
    }

    @Test
    void shouldReturnNotFoundWhenShortKeyDoesNotExist() throws Exception {
        String shortKey = "invalid";
        when(urlService.getOriginalUrl(shortKey))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        mockMvc.perform(get("/" + shortKey))
                .andExpect(status().isNotFound()) // Expect HTTP 404
                .andExpect(jsonPath("$.status").value(404)) // Verifying our GlobalExceptionHandler JSON format!
                .andExpect(jsonPath("$.message").value("URL not found"));
    }
}