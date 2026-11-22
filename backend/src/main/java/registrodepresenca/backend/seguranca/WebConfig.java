package registrodepresenca.backend.seguranca;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@OpenAPIDefinition(security = @SecurityRequirement(name = "bearer"))
@SecurityScheme(name = "bearer", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "token")
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor autenticacao;

    public WebConfig(AuthInterceptor autenticacao) {
        this.autenticacao = autenticacao;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // /api/login e /api/presencas/leitura (usada pelo leitor de cartão) ficam abertas
        registry.addInterceptor(autenticacao)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/login", "/api/presencas/leitura");
    }
}
