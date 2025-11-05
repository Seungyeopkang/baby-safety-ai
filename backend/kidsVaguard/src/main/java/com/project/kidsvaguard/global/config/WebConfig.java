package com.project.kidsvaguard.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    /*
    성공하면 uploads/ 폴더에 이미지가 저장됨
    DB에는 filePath 칼럼에 /uploads/파일명.jpg가 저장됨
    브라우저에서 http://localhost:8080/uploads/파일명.jpg로 접근 가능
     */

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 절대 경로 설정 (uploads 디렉토리를 루트에 둔 경우)
        String absolutePath = "file:" + System.getProperty("user.dir") + "/uploads/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolutePath);
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")  // 모든 Origin 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}
