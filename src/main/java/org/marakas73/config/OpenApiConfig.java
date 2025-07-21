package org.marakas73.config;

import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenApiCustomizer hideClassPropertyCustomizer() {
        return openApi -> {
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values().forEach(schema -> {
                    Map<String, Schema<?>> properties = schema.getProperties();
                    if (properties != null) {
                        properties.remove("@class");
                    }
                });
            }
        };
    }
}
