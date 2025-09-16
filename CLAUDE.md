# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot REST API for generating financial reports in multiple formats (HTML, CSV, PDF). Built with Spring Boot 3.2.0, Java 17, Maven, and uses Thymeleaf templating with Playwright for PDF generation.

## Development Commands

```bash
mvn clean install       # Build and install dependencies
mvn test                # Run all tests (unit + integration)
mvn spring-boot:run     # Run the application on port 8080
mvn clean package       # Clean build and package
mvn test -Dtest=ClassName  # Run specific test class
```

## API Endpoints

### POST /reports
Generates reports from uploaded data files.

**Request:**
- `file` (multipart/form-data): JSON file containing report data
- `template` (form parameter): Template name (currently: "statement")
- `output` (form parameter): Output format (HTML, CSV, PDF)
- `language` (form parameter): Two-letter ISO language code (en, fr, sr, hr)

**Response:**
- HTML: `text/html` content
- CSV: `text/csv` content with download headers
- PDF: `application/pdf` binary with attachment headers

**Sample curl:**
```bash
curl -X POST http://localhost:8080/reports \
  -F "file=@sample-statement.json" \
  -F "template=statement" \
  -F "output=HTML" \
  -F "language=en"
```

### GET /templates
Returns available templates and their supported output formats.

**Response:**
```json
{
  "statement": ["HTML", "CSV", "PDF"]
}
```

## Architecture Implementation

### Core Components

**ReportController** (`com.tvm.reportrendering.controller`)
- REST endpoint handler
- Multipart file upload processing
- Error handling with JSON responses
- Logging of all requests

**ReportService** (`com.tvm.reportrendering.service`)
- Template discovery and registration
- Report generation coordination
- Handler mapping by template name
- Business logic orchestration
- Language parameter validation and propagation

**Report Abstract Class** (`com.tvm.reportrendering.service.Report`)
- Strategy pattern base class
- Generic `process()` method implementation
- Template engine integration
- PDF service coordination
- Language file loading and validation
- Internationalization label management
- Null-safe error handling

**StatementReport** (`com.tvm.reportrendering.service.StatementReport`)
- Concrete implementation for financial statements
- JSON parsing with Jackson ObjectMapper
- Account balance calculations (opening/closing)
- Transaction sorting and grouping

**PdfService** (`com.tvm.reportrendering.service.PdfService`)
- Playwright integration for PDF generation
- Chromium browser automation
- HTML to PDF conversion with headers/footers
- Binary content handling

### Report Processing Flow

1. **File Upload**: Controller receives multipart file and parameters (including language)
2. **Language Validation**: Controller validates two-letter ISO language code format
3. **Service Dispatch**: ReportService finds appropriate handler by template name
4. **Data Parsing**: Concrete Report class parses input stream to model objects
5. **Language Loading**: Report class loads language labels from JSON files
6. **Balance Calculation**: Business logic calculates account opening/closing balances
7. **Template Rendering**: Thymeleaf processes templates with model data and language labels
8. **Format Output**:
   - HTML: Direct template output with internationalized labels
   - CSV: Template-generated CSV format with translated headers
   - PDF: HTML → Playwright → Binary PDF with localized content

### Data Models

**StatementModel** (`com.tvm.reportrendering.model`)
```java
{
  startDate: LocalDate,
  endDate: LocalDate,
  totalOpeningBalance: BigDecimal,
  totalClosingBalance: BigDecimal,
  accounts: List<Account>
}
```

**Account** (`com.tvm.reportrendering.model`)
```java
{
  accountName: String,
  transitNumber: String,
  accountNumber: String,
  accountType: String,
  openingBalance: BigDecimal,
  closingBalance: BigDecimal,
  transactions: List<Transaction>
}
```

**Transaction** (`com.tvm.reportrendering.model`)
```java
{
  actionDate: LocalDate,
  valueDate: LocalDate,
  transactionType: String,
  description: String,
  creditAmount: BigDecimal,
  debitAmount: BigDecimal,
  balance: BigDecimal
}
```

**ReportOutput** (`com.tvm.reportrendering.model`)
```java
{
  mimeType: String,
  content: Object  // String for HTML/CSV, byte[] for PDF
}
```

### Template Structure

```
src/main/resources/templates/statement/
├── html.html           # HTML report template
├── csv.html            # CSV generation template
├── pdf.html            # PDF main content
├── pdf_header.html     # PDF header (optional)
├── pdf_footer.html     # PDF footer (optional)
├── language_en.json    # English language labels
├── language_fr.json    # French (Quebec) language labels
├── language_sr.json    # Serbian Cyrillic language labels
└── language_hr.json    # Croatian language labels
```

**Template Features:**
- Thymeleaf expressions for data binding
- Account iteration with transaction details
- Balance calculations and formatting
- Conditional rendering for different account types
- RBC logo integration in headers
- Responsive table layouts
- Internationalization support with language-specific labels

### Internationalization (i18n)

**Language Support:**
- English (en) - Default language
- French (fr) - Canadian French (Quebec)
- Serbian (sr) - Cyrillic script
- Croatian (hr) - Latin script

**Language File Structure:**
```json
{
  "statement_title": "Account Statement",
  "statement_period": "Statement Period:",
  "account_number": "Account Number:",
  "transit_number": "Transit Number:",
  "account_type": "Account Type:",
  "opening_balance": "Opening Balance:",
  "closing_balance": "Closing Balance:",
  "transactions": "Transactions",
  "action_date": "Action Date",
  "value_date": "Value Date",
  "description": "Description",
  "type": "Type",
  "credit": "Credit",
  "debit": "Debit",
  "balance": "Balance",
  "total_opening_balance": "Total Opening Balance:",
  "total_closing_balance": "Total Closing Balance:",
  "to": "to"
}
```

**Language Integration:**
- Language files stored in `templates/{template_name}/language_{code}.json`
- Labels loaded dynamically based on language parameter
- Template variables accessible as `${labels.key_name}`
- Automatic validation and error handling for missing language files
- Exception thrown if language file not found for requested language

### Configuration Classes

**ThymeleafConfig** (`com.tvm.reportrendering.config`)
- Template engine configuration
- Class path resource resolver
- UTF-8 character encoding
- Template caching settings

**@ReportName Annotation** (`com.tvm.reportrendering.annotation`)
- Custom annotation for template identification
- Component stereotype for Spring discovery
- Template-to-class mapping

### Error Handling

**Global Exception Handler** (`com.tvm.reportrendering.controller.ReportController`)
- IllegalArgumentException → 400 Bad Request
- RuntimeException → 500 Internal Server Error
- Structured JSON error responses
- Request logging for debugging

### Testing Implementation

**Unit Tests:**
- `ReportServiceTest`: Mock-based service testing
- `ReportControllerTest`: MockMvc REST API testing
- `StatementReportTest`: Business logic testing
- `ReportOutputTest`: Model validation testing

**Integration Tests:**
- `ReportServiceIntegrationTest`: Full service layer testing
- `ReportControllerIntegrationTest`: Complete REST API testing
- `ReportRenderingApiIntegrationTest`: End-to-end application testing

**Test Data:**
- `integration-test-data.json`: Realistic financial data with 3 accounts
- Comprehensive transaction scenarios
- Edge cases for balance calculations

### Sample Data Format

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

### Dependencies (pom.xml)

**Core:**
- Spring Boot Starter Web (3.2.0)
- Spring Boot Starter Thymeleaf
- Jackson Databind (JSON processing)

**PDF Generation:**
- Playwright Java (browser automation)

**Testing:**
- Spring Boot Starter Test
- JUnit 5, Mockito, AssertJ

**Utilities:**
- Lombok (boilerplate reduction)
- Spring Boot DevTools (development)

### Key Implementation Notes

1. **Null Safety**: All debit/credit amount handling includes null checks
2. **Balance Calculations**: Opening balance = first transaction balance - first transaction amount
3. **PDF Generation**: Requires Playwright Chromium browser download on first run
4. **Template Discovery**: Spring component scanning finds @ReportName annotated classes
5. **Error Propagation**: All exceptions wrapped with meaningful messages
6. **Logging**: Extensive DEBUG/INFO logging throughout processing pipeline
7. **Testing**: 48 total tests covering unit, integration, and end-to-end scenarios

### Production Considerations

- PDF generation may require additional Chromium dependencies in containerized environments
- Template caching can be enabled for production performance
- File upload size limits should be configured based on expected data volumes
- Consider async processing for large reports
- Add authentication/authorization as needed
- Monitor memory usage for PDF generation