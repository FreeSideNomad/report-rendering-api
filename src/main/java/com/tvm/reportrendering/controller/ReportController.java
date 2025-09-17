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

import static com.tvm.reportrendering.util.SecurityUtils.sanitizeForLogging;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Reports", description = "API for generating financial reports in multiple formats")
public class ReportController {

    private final ReportService reportService;


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

        // Validate language code format
        if (language == null || !language.matches("^[a-z]{2}$")) {
            log.error("Invalid language code: {}", sanitizeForLogging(language));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid language code. Must be a two-letter ISO language code (e.g., en, fr, sr, hr)"));
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