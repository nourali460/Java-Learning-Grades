package com.nour.ali.java_learning_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "https://cslearningplatform.com",
                        "https://www.cslearningplatform.com",
                        "http://cslearningplatform.com",
                        "http://www.cslearningplatform.com",          
                        "https://nourali460.github.io",
                        "http://localhost:*"
                )
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

}
