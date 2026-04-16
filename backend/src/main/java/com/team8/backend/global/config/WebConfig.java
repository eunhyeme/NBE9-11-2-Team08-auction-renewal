package com.team8.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** 로 들어오는 요청은 서버 로컬의 uploads/ 폴더에서 찾겠다는 설정입니다.
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}