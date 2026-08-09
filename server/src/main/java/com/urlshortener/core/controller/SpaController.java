package com.urlshortener.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    // Forward known frontend routes to index.html so React Router handles them
    @GetMapping({"/", "/dashboard", "/admin"})
    public String forwardSpaRoutes() {
        return "forward:/index.html";
    }
}
