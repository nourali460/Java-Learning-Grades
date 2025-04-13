package com.nour.ali.java_learning_backend.config;

import com.nour.ali.java_learning_backend.security.CustomAuthenticationEntryPoint;
import com.nour.ali.java_learning_backend.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomAuthenticationEntryPoint customEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ use your cors config
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                )

                .exceptionHandling(e -> e.authenticationEntryPoint(customEntryPoint))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("*")); // ✅ allow all origins
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // ✅ include OPTIONS
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // ✅ only works with allowedOriginPatterns

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {

                String method = request.getMethod();
                String uri = request.getRequestURI();
                System.out.println("➡️ Incoming request: " + method + " " + uri);

                // ✅ Let CORS handle preflight fully
                if ("OPTIONS".equalsIgnoreCase(method)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    try {
                        String username = jwtService.extractUsername(token);
                        String role = jwtService.extractRole(token);
                        boolean valid = jwtService.validateToken(token);

                        if (username != null && valid &&
                                SecurityContextHolder.getContext().getAuthentication() == null) {

                            var auth = new UsernamePasswordAuthenticationToken(
                                    username, null, Collections.emptyList());
                            SecurityContextHolder.getContext().setAuthentication(auth);
                            System.out.println("🟢 Authenticated: " + username);
                        }

                    } catch (JwtException e) {
                        System.out.println("❌ JWT error: " + e.getMessage());
                    }
                }

                filterChain.doFilter(request, response);
            }
        };
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
