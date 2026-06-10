package org.petmeet.config;

import org.springframework.context.annotation.Configuration;
import org.petmeet.support.UploadPathResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(UploadPathResolver.toResourceLocation(UploadPathResolver.resolveUploadsRootDir()));
    }
}
