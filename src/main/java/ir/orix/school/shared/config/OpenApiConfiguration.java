package ir.orix.school.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI schoolPlatformOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Orix School Platform API")
                .description("REST API for the Orix School Platform")
                .version("v1"));
    }
}
