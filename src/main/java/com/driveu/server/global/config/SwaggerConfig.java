package com.driveu.server.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@OpenAPIDefinition(
        info = @Info(
                title = "API 명세서",
                description = "DriveU 프로젝트의 API 명세서입니다.",
                version = "v2"
        )
)
@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI openAPI(){

        SecurityScheme apiKey = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .in(SecurityScheme.In.HEADER)
                .name("Authorization")
                .scheme("bearer")
                .bearerFormat("JWT");

        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("Bearer Token");


        Server prodServer = new Server();
        prodServer.setUrl("http://43.202.232.100:8080");
        prodServer.setDescription("AWS EC2 서버");

        Server localServer = new Server();
        localServer.setUrl("http://localhost:8080");
        localServer.setDescription("Local server for testing");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("Bearer Token", apiKey))
                .addSecurityItem(securityRequirement)
                .servers(List.of(localServer, prodServer));}
    ;

}

