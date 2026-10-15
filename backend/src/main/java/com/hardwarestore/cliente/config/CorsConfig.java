package com.hardwarestore.cliente.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera o acesso da API para o frontend rodando localmente (ex.: live-server,
 * Vite, http-server) e para portas encaminhadas pelo GitHub Codespaces, cujo
 * dominio e dinamico (ex.: https://SEU-CODESPACE-5600.app.github.dev).
 *
 * Em produção, restrinja allowedOriginPatterns ao(s) dominio(s) reais do
 * frontend em vez de aceitar qualquer porta/subdominio.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://*.app.github.dev",
                        "https://*.github.dev"
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
