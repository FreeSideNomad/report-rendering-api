package com.tvm.reportrendering.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");

        Server productionServer = new Server()
                .url("https://api.example.com")
                .description("Production server");

        Contact contact = new Contact()
                .name("TVM Software")
                .email("igormusic@tvmsoftware.com")
                .url("https://tvmsoftware.com");

        License license = new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Report Rendering API")
                .version("1.0.0")
                .description("""
                        REST API for generating financial reports in multiple formats (HTML, CSV, PDF).

                        ## Features
                        - Upload JSON financial data
                        - Generate reports in HTML, CSV, or PDF format
                        - Template-based report generation
                        - High-performance PDF generation with Playwright

                        ## Usage
                        1. Use GET /api/templates to see available templates
                        2. Upload your JSON data file to POST /api/reports
                        3. Specify the template and output format
                        4. Download your generated report

                        ## Data Format
                        The API expects JSON files with the following structure:
                        ```json
                        {
                          "startDate": "2024-01-01",
                          "endDate": "2024-01-31",
                          "accounts": [{
                            "accountName": "Account Name",
                            "transitNumber": "00001",
                            "accountNumber": "1234567890",
                            "accountType": "Chequing",
                            "transactions": [...]
                          }]
                        }
                        ```
                        """)
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer, productionServer));
    }
}