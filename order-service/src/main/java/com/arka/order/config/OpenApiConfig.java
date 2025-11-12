package com.arka.order.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile({"dev", "test"})
public class OpenApiConfig {

  @Value("${server.port:8095}")
  private String serverPort;

  @Bean
  public OpenAPI orderServiceOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Order Service API")
            .description("Servicio de gestión de pedidos con patrón SAGA")
            .version("1.0.0")
            .contact(new Contact()
                .name("Arka Team")
                .email("support@arka.com")
                .url("https://arka.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
        .servers(List.of(
            new Server().url("http://localhost:" + serverPort).description("Local Development"),
            new Server().url("http://gateway:8080").description("Gateway")
        ))
        .components(new Components()
            .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token de autenticación")))
        .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
  }
}
