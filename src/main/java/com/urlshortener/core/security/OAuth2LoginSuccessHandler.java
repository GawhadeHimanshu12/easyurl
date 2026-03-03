package com.urlshortener.core.security;

import com.urlshortener.core.entity.Provider;
import com.urlshortener.core.entity.Role;
import com.urlshortener.core.entity.UserEntity;
import com.urlshortener.core.repository.UrlRepository;
import com.urlshortener.core.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final UrlRepository urlRepository;
    private final JwtTokenProvider tokenProvider;

    @Value("${app.admin.emails}")
    private String adminEmailsString;

    @Value("${app.jwt.cookie-name}")
    private String cookieName;

    @Value("${app.cookie.secure}")
    private boolean cookieSecure;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        log.info("OAuth login success for email: {}", email);

        // 1. Process User
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> {
            List<String> adminEmails = Arrays.asList(adminEmailsString.split(","));
            Role assignedRole = adminEmails.contains(email) ? Role.ADMIN : Role.USER;

            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .name(name)
                    .provider(Provider.GOOGLE)
                    .role(assignedRole)
                    .build();
            return userRepository.save(newUser);
        });

        // 2. Claim Anonymous URLs
        // Note: Browsers drop custom headers during OAuth redirects. We strictly read the cookie here.
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("anon_id".equals(cookie.getName())) {
                    log.info("Found anon_id cookie. Claiming URLs for user {}", user.getEmail());
                    urlRepository.claimAnonymousUrls(user, cookie.getValue());

                    // Clear the anon cookie
                    Cookie clearCookie = new Cookie("anon_id", null);
                    clearCookie.setMaxAge(0);
                    clearCookie.setPath("/");
                    response.addCookie(clearCookie);
                    break;
                }
            }
        }

        // 3. Generate JWT and Set Cookie
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        Cookie jwtCookie = new Cookie(cookieName, token);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(cookieSecure);
        jwtCookie.setPath("/");
        jwtCookie.setAttribute("SameSite", "Lax");
        jwtCookie.setMaxAge(3600); // 1 hour

        response.addCookie(jwtCookie);

        // 4. Redirect to Frontend (We redirect to our API home for testing)
        getRedirectStrategy().sendRedirect(request, response, "/api/v1/urls/my-urls");
    }
}