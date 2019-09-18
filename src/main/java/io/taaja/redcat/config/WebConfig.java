package io.taaja.redcat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    //https://spring.io/blog/2015/06/08/cors-support-in-spring-framework
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                .addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "HEAD")
                //.allowedOrigins("http://localhost:8080")
                .allowCredentials(true);
    }
}
