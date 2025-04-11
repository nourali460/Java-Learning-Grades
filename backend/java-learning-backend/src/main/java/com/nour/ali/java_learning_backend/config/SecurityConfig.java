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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .exceptionHandling(e -> e.authenticationEntryPoint(customEntryPoint))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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
                        .requestMatchers(HttpMethod.GET, "/admins/students/passwords").authenticated()
                        .requestMatchers(HttpMethod.POST, "/grades").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://nourali460.github.io"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

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
                System.out.println("\u27a1\ufe0f Incoming request: " + method + " " + uri);

                System.out.println("\ud83d\udce6 Headers:");
                Collections.list(request.getHeaderNames()).forEach(name ->
                        System.out.println("   → " + name + ": " + request.getHeader(name)));

                String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    System.out.println("\ud83d\udd10 Token received: " + token);

                    try {
                        String username = jwtService.extractUsername(token);
                        String role = jwtService.extractRole(token);
                        boolean valid = jwtService.validateToken(token);

                        System.out.println("\ud83e\uddd0 Username: " + username);
                        System.out.println("\ud83c\udfad Role: " + role);
                        System.out.println("\u2705 Is token valid? " + valid);

                        if (username != null &&
                                valid &&
                                SecurityContextHolder.getContext().getAuthentication() == null) {

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(
                                            username, null, Collections.emptyList());

                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            System.out.println("\ud83d\udfe2 Authentication successful for: " + username);
                        } else {
                            System.out.println("\u26a0\ufe0f Token invalid or already authenticated.");
                        }

                    } catch (JwtException e) {
                        System.out.println("\u274c JWT error: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.out.println("\u274c Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }

                } else {
                    System.out.println("\u2757 No Authorization header or malformed header.");
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