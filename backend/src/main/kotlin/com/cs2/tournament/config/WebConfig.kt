package com.cs2.tournament.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Map requests to /uploads/** to the absolute path of the uploads directory in the project root
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/")
    }
}
