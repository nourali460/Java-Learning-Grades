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
                                "/api/stripe/**",
                                "/",
                                "/index.html",
                                "/assets/**",
                                "/static/**"
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
                String method = request.getMethod();
                String uri = request.getRequestURI();
                System.out.println("➡️ Incoming request: " + method + " " + uri);

                // Print all headers for debugging
                System.out.println("📦 Headers:");
                Collections.list(request.getHeaderNames()).forEach(name ->
                        System.out.println("   → " + name + ": " + request.getHeader(name)));

                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    System.out.println("🔐 Token received: " + token);

                    try {
                        String username = jwtService.extractUsername(token);
                        String role = jwtService.extractRole(token);
                        boolean valid = jwtService.validateToken(token);

                        System.out.println("🧠 Username: " + username);
                        System.out.println("🎭 Role: " + role);
                        System.out.println("✅ Is token valid? " + valid);

                        if (username != null &&
                                valid &&
                                SecurityContextHolder.getContext().getAuthentication() == null) {

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            username, null, Collections.emptyList());

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("🟢 Authentication successful for: " + username);
                        } else {
                            System.out.println("⚠️ Token invalid or already authenticated.");
                        }

                    } catch (JwtException e) {
                        System.out.println("❌ JWT error: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.out.println("❌ Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }

                } else {
                    System.out.println("❗ No Authorization header or malformed header.");
                    if (authHeader != null) {
                        System.out.println("   ⛔ Found header: " + authHeader);
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
