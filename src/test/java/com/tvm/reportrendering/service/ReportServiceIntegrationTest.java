package com.tvm.reportrendering.service;

import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportServiceIntegrationTest {

    @Autowired
    private ReportService reportService;

    @Test
    void testGetAvailableTemplatesIntegration() {
        Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();

        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.containsKey("statement"));

        List<OutputFormat> statementFormats = templates.get("statement");
        assertEquals(3, statementFormats.size());
        assertTrue(statementFormats.contains(OutputFormat.HTML));
        assertTrue(statementFormats.contains(OutputFormat.CSV));
        assertTrue(statementFormats.contains(OutputFormat.PDF));
    }

    @Test
    void testGenerateStatementReportHtmlIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        try (InputStream inputStream = resource.getInputStream()) {
            ReportOutput output = reportService.generateReport(inputStream, "statement", OutputFormat.HTML);

            assertNotNull(output);
            assertEquals("text/html", output.getMimeType());
            assertNotNull(output.getContent());
            assertTrue(output.getContent() instanceof String);

            String htmlContent = (String) output.getContent();
            assertFalse(htmlContent.isEmpty());

            // Verify HTML structure
            assertTrue(htmlContent.contains("<html"));
            assertTrue(htmlContent.contains("</html>"));
            assertTrue(htmlContent.contains("John Smith Checking Account"));
            assertTrue(htmlContent.contains("1234567890"));
            assertTrue(htmlContent.contains("Salary Deposit"));
            assertTrue(htmlContent.contains("5000.00"));
        }
    }

    @Test
    void testGenerateStatementReportCsvIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        try (InputStream inputStream = resource.getInputStream()) {
            ReportOutput output = reportService.generateReport(inputStream, "statement", OutputFormat.CSV);

            assertNotNull(output);
            assertEquals("text/csv", output.getMimeType());
            assertNotNull(output.getContent());
            assertTrue(output.getContent() instanceof String);

            String csvContent = (String) output.getContent();
            assertFalse(csvContent.isEmpty());

            // Verify CSV structure
            assertTrue(csvContent.contains("Account Number"));
            assertTrue(csvContent.contains("Date"));
            assertTrue(csvContent.contains("Description"));
            assertTrue(csvContent.contains("Amount"));
            assertTrue(csvContent.contains("1234567890"));
            assertTrue(csvContent.contains("Salary Deposit"));
            assertTrue(csvContent.contains("5000.00"));
        }
    }

    @Test
    void testGenerateStatementReportPdfIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        try (InputStream inputStream = resource.getInputStream()) {
            ReportOutput output = reportService.generateReport(inputStream, "statement", OutputFormat.PDF);

            assertNotNull(output);
            assertEquals("application/pdf", output.getMimeType());
            assertNotNull(output.getContent());
            assertTrue(output.getContent() instanceof byte[]);

            byte[] pdfContent = (byte[]) output.getContent();
            assertTrue(pdfContent.length > 0);

            // Verify PDF header (basic check)
            String pdfHeader = new String(pdfContent, 0, Math.min(10, pdfContent.length));
            assertTrue(pdfHeader.startsWith("%PDF"));
        }
    }

    @Test
    void testGenerateReportWithMultipleAccountsIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        try (InputStream inputStream = resource.getInputStream()) {
            ReportOutput output = reportService.generateReport(inputStream, "statement", OutputFormat.HTML);

            assertNotNull(output);
            String htmlContent = (String) output.getContent();

            // Verify all three accounts are included
            assertTrue(htmlContent.contains("1234567890")); // Checking
            assertTrue(htmlContent.contains("0987654321")); // Savings
            assertTrue(htmlContent.contains("4111111111111111")); // Credit Card

            // Verify account types
            assertTrue(htmlContent.contains("Checking"));
            assertTrue(htmlContent.contains("Savings"));
            assertTrue(htmlContent.contains("Credit Card"));

            // Verify transactions from different accounts
            assertTrue(htmlContent.contains("Salary Deposit")); // From checking
            assertTrue(htmlContent.contains("Interest Payment")); // From savings
            assertTrue(htmlContent.contains("Online Purchase")); // From credit card
        }
    }

    @Test
    void testGenerateReportWithInvalidTemplateIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        try (InputStream inputStream = resource.getInputStream()) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                reportService.generateReport(inputStream, "invalid-template", OutputFormat.HTML);
            });

            assertTrue(exception.getMessage().contains("No report handler found for template: invalid-template"));
        }
    }

    @Test
    void testGenerateReportWithInvalidJsonIntegration() {
        String invalidJson = "{invalid json content}";
        InputStream inputStream = new java.io.ByteArrayInputStream(invalidJson.getBytes());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            reportService.generateReport(inputStream, "statement", OutputFormat.HTML);
        });

        assertTrue(exception.getMessage().contains("Failed to process report"));
    }

    @Test
    void testReportServiceInitializationIntegration() {
        // Verify that the service initializes properly and finds the statement report handler
        Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();

        assertNotNull(templates);
        assertEquals(1, templates.size());
        assertTrue(templates.containsKey("statement"));

        List<OutputFormat> formats = templates.get("statement");
        assertEquals(3, formats.size());
    }
}