package com.example.myapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI triageEngineOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title("Emergency Triage Priority Engine API")
                                                .description(
                                                                "A rule-based backend system for rapid, consistent patient triage in emergency departments. "
                                                                                +
                                                                                "Submit patient vitals and symptoms to receive an automated priority classification: "
                                                                                +
                                                                                "**CRITICAL**, **HIGH**, **MEDIUM**, or **LOW**.\n\n"
                                                                                +
                                                                                "### Scoring Categories\n" +
                                                                                "| Category | Max Points |\n" +
                                                                                "|----------|------------|\n" +
                                                                                "| Vital Signs Abnormality | 40 |\n" +
                                                                                "| High-Risk Symptom Flags | 30 |\n" +
                                                                                "| Age Vulnerability | 15 |\n" +
                                                                                "| Chief Complaint Keywords | 15 |\n\n"
                                                                                +
                                                                                "### Priority Thresholds\n" +
                                                                                "| Level | Score Range |\n" +
                                                                                "|-------|-------------|\n" +
                                                                                "| CRITICAL | >= 75 |\n" +
                                                                                "| HIGH | 50-74 |\n" +
                                                                                "| MEDIUM | 25-49 |\n" +
                                                                                "| LOW | < 25 |")
                                                .version("1.0.0")
                                                .contact(new Contact()
                                                                .name("Triage Engine Team")
                                                                .email("triage@hospital.com"))
                                                .license(new License()
                                                                .name("MIT License")))
                                .servers(List.of(
                                                new Server().url("http://localhost:8080")
                                                                .description("Local Development Server")));
        }
}
