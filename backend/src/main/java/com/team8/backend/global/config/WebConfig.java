package com.team8.backend.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 💡 프로젝트 루트 폴더 기준 uploads 폴더의 절대 경로를 가져옵니다.
        String path = new File(uploadDir).getAbsolutePath();

        // 💡 윈도우 경로를 URL 형태로 변환할 때 마지막에 슬래시를 붙여주는 것이 안전합니다.
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + path);
    }
}