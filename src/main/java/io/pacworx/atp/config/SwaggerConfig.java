package io.pacworx.atp.config;

import com.google.common.collect.Lists;
import io.pacworx.atp.user.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.ApiKeyVehicle;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.List;

import static com.google.common.base.Predicates.or;
import static springfox.documentation.builders.PathSelectors.regex;

/**
 * Swagger API documentation
 * Author: Max Tuzzolino
 *
 * Instructions: Simply start server and navigate to localhost:8080/swagger-ui.html
 */

@Configuration
@EnableSwagger2
@ComponentScan("io.pacworx.atp")
public class SwaggerConfig {
    @Bean
    public Docket allApis() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                    .select()
                    .paths(or (regex("/app.*"),
                            regex("/auth.*")))
                    .build()
                .securitySchemes(Lists.newArrayList(apiKey()))
                .securityContexts(Lists.newArrayList(securityContext()));

    }

    private ApiKey apiKey() {
        return new ApiKey("Authorization", "jwt", "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(or (regex("/api.*")))
                .build();
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
                = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Lists.newArrayList(
                new SecurityReference("Authorization", authorizationScopes));
    }

    // OAuth mocked for JWT
    @Bean
    public SecurityConfiguration security() {
        return new SecurityConfiguration(
                "",
                "",
                "",
                "Ask the People",
                "bearer ",
                ApiKeyVehicle.HEADER,
                "jwt",
                "," /*scope separator*/);
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Ask the People API")
                .description("Documentation for Ask the People APIs. " +
                        "Use authentication APIs to be issued a JWT token. Add that token to " +
                        "the API input in the top right to access authorized APIs.")
                .termsOfServiceUrl("http://www-03.ibm.com/software/sla/sladb.nsf/sla/bm?Open")
                .license("Apache License Version 2.0")
                .licenseUrl("askthepeople.io")
                .version("2.0")
                .build();
    }
}
