package com.tvm.reportrendering.controller;

import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import com.tvm.reportrendering.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.tvm.reportrendering.util.SecurityUtils.sanitizeForLogging;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Reports", description = "API for generating financial reports in multiple formats")
public class ReportController {

    private final ReportService reportService;

    // Pattern to validate template names - only alphanumeric characters and underscores
    private static final Pattern TEMPLATE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    // Pattern to validate language codes - two lowercase letters only
    private static final Pattern LANGUAGE_CODE_PATTERN = Pattern.compile("^[a-z]{2}$");

    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace('\r', '_').replace('\n', '_').replace('\t', '_');
    }

    /**
     * Validates template name to prevent Server-side Template Injection (SSTI) attacks.
     * Uses available templates from ReportService to create a dynamic whitelist.
     *
     * @param templateName the template name to validate
     * @throws IllegalArgumentException if template name is invalid or not allowed
     */
    private void validateTemplateName(String templateName) {
        if (templateName == null || templateName.trim().isEmpty()) {
            throw new IllegalArgumentException("Template name cannot be null or empty");
        }

        // Get available templates from the service (dynamic whitelist)
        Set<String> allowedTemplates = reportService.getAvailableTemplates().keySet();

        // Check against whitelist of allowed template names
        if (!allowedTemplates.contains(templateName)) {
            log.error("Attempted access to unauthorized template: {}", sanitizeForLogging(templateName));
            throw new IllegalArgumentException("Template not allowed: " + templateName);
        }

        // Additional pattern validation to prevent path traversal and injection
        if (!TEMPLATE_NAME_PATTERN.matcher(templateName).matches()) {
            log.error("Template name contains invalid characters: {}", sanitizeForLogging(templateName));
            throw new IllegalArgumentException("Template name contains invalid characters");
        }

        // Prevent path traversal attempts
        if (templateName.contains("..") || templateName.contains("/") || templateName.contains("\\")) {
            log.error("Template name contains path traversal attempt: {}", sanitizeForLogging(templateName));
            throw new IllegalArgumentException("Template name contains forbidden path characters");
        }
    }

    /**
     * Validates language code to prevent injection attacks.
     *
     * @param language the language code to validate
     * @throws IllegalArgumentException if language code is invalid
     */
    private void validateLanguageCode(String language) {
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("Language code cannot be null or empty");
        }

        if (!LANGUAGE_CODE_PATTERN.matcher(language).matches()) {
            log.error("Invalid language code format: {}", sanitizeForLogging(language));
            throw new IllegalArgumentException("Language code must be a two-letter lowercase code");
        }
    }

    @Operation(
            summary = "Generate a financial report",
            description = "Upload a JSON file containing financial data and generate a report in the specified format (HTML, CSV, or PDF)"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Report generated successfully",
                    content = {
                            @Content(mediaType = "text/html", schema = @Schema(type = "string"), examples = @ExampleObject(name = "HTML Report", value = "<html>...</html>")),
                            @Content(mediaType = "text/csv", schema = @Schema(type = "string"), examples = @ExampleObject(name = "CSV Report", value = "Account,Balance\nChequing,1000.00")),
                            @Content(mediaType = "application/pdf", schema = @Schema(type = "string", format = "binary"))
                    }
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters or file format"),
            @ApiResponse(responseCode = "500", description = "Internal server error during report generation")
    })
    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateReport(
            @Parameter(description = "JSON file containing financial data", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Template name for the report", example = "statement", required = true)
            @RequestParam("template") String template,
            @Parameter(description = "Output format for the report", required = true)
            @RequestParam("output") OutputFormat output,
            @Parameter(description = "Two-letter ISO language code", example = "en", required = true)
            @RequestParam("language") String language) {

        log.info("Received report generation request: template={}, output={}, language={}, file={}",
                sanitizeForLogging(template), output, sanitizeForLogging(language), sanitizeForLogging(file.getOriginalFilename()));

        // Validate template name and language code to prevent SSTI and injection attacks
        try {
            validateTemplateName(template);
            validateLanguageCode(language);
        } catch (IllegalArgumentException e) {
            log.error("Validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }

        try {
            ReportOutput reportOutput = reportService.generateReport(
                    file.getInputStream(),
                    template,
                    output,
                    language
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(reportOutput.getMimeType()));

            if (output == OutputFormat.PDF) {
                headers.add("Content-Disposition",
                        String.format("attachment; filename=\"%s-report.pdf\"", template));
                return new ResponseEntity<>(reportOutput.getContentAsByteArray(), headers, HttpStatus.OK);
            } else if (output == OutputFormat.CSV) {
                headers.add("Content-Disposition",
                        String.format("attachment; filename=\"%s-report.csv\"", template));
                return new ResponseEntity<>(reportOutput.getContentAsString(), headers, HttpStatus.OK);
            } else {
                // HTML
                return new ResponseEntity<>(reportOutput.getContentAsString(), headers, HttpStatus.OK);
            }

        } catch (IOException e) {
            log.error("Error reading uploaded file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid request parameters: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid request parameters", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Report generation failed", "message", e.getMessage()));
        }
    }

    @Operation(
            summary = "Get available report templates",
            description = "Retrieve a list of all available report templates and their supported output formats"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Templates retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    type = "object",
                                    example = "{\"statement\": [\"HTML\", \"CSV\", \"PDF\"]}"
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error while retrieving templates")
    })
    @GetMapping("/templates")
    public ResponseEntity<Map<String, List<OutputFormat>>> getAvailableTemplates() {
        log.info("Received request for available templates");

        try {
            Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Error retrieving templates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}