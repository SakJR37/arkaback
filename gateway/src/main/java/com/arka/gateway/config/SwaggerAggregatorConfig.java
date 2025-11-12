package com.arka.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile({"dev", "test"})
public class SwaggerAggregatorConfig {

  @Bean
  public List<GroupedOpenApi> apis() {
    List<GroupedOpenApi> groups = new ArrayList<>();
    
    groups.add(GroupedOpenApi.builder()
        .group("auth-service")
        .pathsToMatch("/auth/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("cart-service")
        .pathsToMatch("/api/cart/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("catalog-service")
        .pathsToMatch("/api/catalog/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("inventory-service")
        .pathsToMatch("/api/inventory/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("order-service")
        .pathsToMatch("/api/orders/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("notification-service")
        .pathsToMatch("/api/notifications/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("review-service")
        .pathsToMatch("/api/reviews/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("shipping-service")
        .pathsToMatch("/api/shipping/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("provider-service")
        .pathsToMatch("/api/providers/**")
        .build());
    
    groups.add(GroupedOpenApi.builder()
        .group("category-service")
        .pathsToMatch("/api/categories/**")
        .build());
    
    return groups;
  }
}
