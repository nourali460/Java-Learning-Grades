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
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

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
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .exceptionHandling(e -> e.authenticationEntryPoint(customEntryPoint))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/h2-console/**",
                                "/admins/validate",
                                "/students/validate",
                                "/roles",
                                "/admins",
                                "/admins/contains",
                                "/api/stripe/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/grades").permitAll()
                        .requestMatchers(HttpMethod.POST, "/grades").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                System.out.println("🔍 Incoming request to: " + request.getMethod() + " " + request.getRequestURI());

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    System.out.println("🔐 Authorization header found. Token: " + token);

                    try {
                        String username = jwtService.extractUsername(token);
                        String role = jwtService.extractRole(token);

                        System.out.println("🔍 Extracted Username: " + username + " | Role: " + role);

                        if (username != null &&
                                jwtService.validateToken(token) &&
                                SecurityContextHolder.getContext().getAuthentication() == null) {

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            username,
                                            null,
                                            Collections.emptyList()
                                    );

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("✅ JWT authenticated and security context set for: " + username);
                            System.out.println("🔐 SecurityContext now contains: " +
                                    SecurityContextHolder.getContext().getAuthentication());
                        } else {
                            System.out.println("⚠️ JWT validation failed or already authenticated.");
                        }

                    } catch (JwtException e) {
                        System.out.println("❌ JWT exception: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("❌ Unexpected exception: " + e.getMessage());
                    }
                } else {
                    System.out.println("❗ No valid Authorization header found.");
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
