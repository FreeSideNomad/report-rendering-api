# Report Rendering API

A Spring Boot REST API for generating financial reports in multiple formats (HTML, CSV, PDF). Built with Spring Boot 3.2.0, Java 17, and uses Thymeleaf templating with Playwright for PDF generation.

## Features

- 📊 Generate financial reports from JSON data
- 📄 Multiple output formats: HTML, CSV, PDF
- 🎨 Template-based rendering with Thymeleaf
- 🚀 High-performance PDF generation with Playwright
- 🔒 Secure file upload handling
- 📱 RESTful API design
- 🐳 Docker containerization with RHEL base
- ✅ Comprehensive test coverage

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (for containerized deployment)

### Local Development

```bash
# Clone the repository
git clone <repository-url>
cd report-rendering-api

# Build and run
mvn clean install
mvn spring-boot:run

# Application will be available at http://localhost:8080
```

### Docker Deployment

```bash
# Build and run with Docker Compose
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f report-rendering-api

# Stop services
docker-compose down
```

### Production Deployment (with Nginx)

```bash
# Run with nginx reverse proxy
docker-compose --profile production up -d
```

## API Documentation

### Generate Report

**Endpoint:** `POST /reports`

Generates a financial report from uploaded JSON data.

**Request Parameters:**
- `file` (multipart): JSON file containing report data
- `template` (form): Template name (currently: "statement")
- `output` (form): Output format ("HTML", "CSV", "PDF")

**Example:**

```bash
curl -X POST http://localhost:8080/reports \
  -F "file=@sample-data.json" \
  -F "template=statement" \
  -F "output=PDF"
```

**Response:**
- HTML: `text/html` content
- CSV: `text/csv` with download headers
- PDF: `application/pdf` binary

### List Templates

**Endpoint:** `GET /templates`

Returns available report templates and supported formats.

**Response:**
```json
{
  "statement": ["HTML", "CSV", "PDF"]
}
```

## Data Format

### Input JSON Structure

```json
{
  "startDate": "2024-01-01",
  "endDate": "2024-01-31",
  "accounts": [
    {
      "accountName": "John Doe Chequing Account",
      "transitNumber": "00001",
      "accountNumber": "1234567890",
      "accountType": "Chequing",
      "transactions": [
        {
          "actionDate": "2024-01-02",
          "valueDate": "2024-01-02",
          "transactionType": "Deposit",
          "description": "Direct Deposit - Salary",
          "creditAmount": 3500.00,
          "debitAmount": null,
          "balance": 5500.00
        }
      ]
    }
  ]
}
```

### Sample Data

Use the included sample data files for testing:
- `integration-test-data.json` - Comprehensive test data with multiple accounts

## Architecture

### Core Components

- **ReportController**: REST API endpoints and request handling
- **ReportService**: Business logic and template orchestration
- **Report Classes**: Template-specific report generation (Strategy pattern)
- **PdfService**: Playwright-based PDF generation
- **Models**: Data transfer objects for reports and accounts

### Template Structure

```
src/main/resources/templates/statement/
├── html.html           # HTML report template
├── csv.html            # CSV generation template
├── pdf.html            # PDF main content
├── pdf_header.html     # PDF header (optional)
└── pdf_footer.html     # PDF footer (optional)
```

## Development

### Build Commands

```bash
mvn clean install       # Build and install dependencies
mvn test                # Run all tests
mvn spring-boot:run     # Run application (port 8080)
mvn clean package       # Clean build and package
mvn test -Dtest=ClassName  # Run specific test
```

### Testing

The project includes comprehensive test coverage:

- **Unit Tests**: Service and controller logic testing
- **Integration Tests**: Full API testing with MockMvc
- **End-to-End Tests**: Complete application testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# Run specific test suite
mvn test -Dtest="*IntegrationTest"
```

### Adding New Templates

1. Create a new report class extending `Report`:

```java
@ReportName("new-template")
@Component
public class NewTemplateReport extends Report {
    // Implementation
}
```

2. Add template files in `src/main/resources/templates/new-template/`
3. Implement the `parseData()` method for your data structure
4. Add tests for the new template

## Configuration

### Application Properties

```properties
# Server configuration
server.port=8080

# Logging
logging.level.com.tvm.reportrendering=DEBUG

# File upload limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Docker Configuration

- **Base Image**: Red Hat UBI 9
- **Java**: OpenJDK 17
- **Memory**: 1.5GB limit (configurable)
- **Health Checks**: Built-in endpoint monitoring
- **Security**: Non-root user execution

### Environment Variables

- `SPRING_PROFILES_ACTIVE`: Active Spring profile
- `JAVA_OPTS`: JVM configuration options
- `PLAYWRIGHT_BROWSERS_PATH`: Browser installation path

## Monitoring & Operations

### Health Checks

The application provides health check endpoints:

- Docker: `http://localhost:8080/templates`
- Spring Actuator: Configure as needed

### Logging

Logs are written to:
- Console (development)
- `/app/logs` directory (Docker)
- Configurable via `logback-spring.xml`

### Performance

- **Memory**: Optimized JVM settings for containers
- **PDF Generation**: Playwright with Chromium browser
- **Template Caching**: Available for production environments
- **Connection Pooling**: Spring Boot defaults

## Security Considerations

- File upload validation and size limits
- Non-root Docker container execution
- Input sanitization for template data
- Error handling without information disclosure
- No sensitive data in logs or responses

## Troubleshooting

### Common Issues

1. **PDF Generation Fails**
   - Ensure Chromium dependencies are installed
   - Check memory limits (PDF generation is memory-intensive)
   - Verify font availability for text rendering

2. **File Upload Issues**
   - Check file size limits in application properties
   - Verify JSON format matches expected structure
   - Ensure multipart form encoding

3. **Template Not Found**
   - Verify template files exist in correct directory structure
   - Check `@ReportName` annotation matches request parameter
   - Confirm Spring component scanning includes your report class

### Docker Issues

```bash
# Check container logs
docker-compose logs -f report-rendering-api

# Access container shell
docker-compose exec report-rendering-api bash

# Rebuild without cache
docker-compose build --no-cache

# Check resource usage
docker stats report-rendering-api
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## License

[Specify your license here]

## Support

For issues and questions:
- Create an issue in the repository
- Contact: igormusic@tvmsoftware.com