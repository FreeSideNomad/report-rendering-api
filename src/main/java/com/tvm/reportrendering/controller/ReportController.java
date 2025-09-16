package com.tvm.reportrendering.controller;

import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import com.tvm.reportrendering.service.ReportService;
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

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping(value = "/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> generateReport(
            @RequestParam("file") MultipartFile file,
            @RequestParam("template") String template,
            @RequestParam("output") OutputFormat output) {

        log.info("Received report generation request: template={}, output={}, file={}",
                template, output, file.getOriginalFilename());

        try {
            ReportOutput reportOutput = reportService.generateReport(
                    file.getInputStream(),
                    template,
                    output
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(reportOutput.getMimeType()));

            if (output == OutputFormat.PDF) {
                headers.setContentDispositionFormData("attachment",
                        String.format("%s-report.pdf", template));
                return new ResponseEntity<>(reportOutput.getContentAsByteArray(), headers, HttpStatus.OK);
            } else if (output == OutputFormat.CSV) {
                headers.setContentDispositionFormData("attachment",
                        String.format("%s-report.csv", template));
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
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating report: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate report: " + e.getMessage()));
        }
    }

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