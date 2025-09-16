package com.tvm.reportrendering;

import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReportRenderingApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ReportService reportService;

    @Test
    void testApplicationStartupIntegration() {
        // Test that the application context loads successfully
        assertNotNull(reportService);

        // Verify that report handlers are initialized
        Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();
        assertNotNull(templates);
        assertFalse(templates.isEmpty());
        assertTrue(templates.containsKey("statement"));
    }

    @Test
    void testFullWorkflowIntegrationHtml() throws Exception {
        // Step 1: Get available templates
        String templatesUrl = "http://localhost:" + port + "/api/templates";
        ResponseEntity<Map> templatesResponse = restTemplate.getForEntity(templatesUrl, Map.class);

        assertEquals(HttpStatus.OK, templatesResponse.getStatusCode());
        assertNotNull(templatesResponse.getBody());
        assertTrue(templatesResponse.getBody().containsKey("statement"));

        // Step 2: Generate HTML report
        String reportsUrl = "http://localhost:" + port + "/api/reports";

        ClassPathResource resource = new ClassPathResource("integration-test-data.json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("template", "statement");
        body.add("output", "HTML");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> reportResponse = restTemplate.postForEntity(reportsUrl, requestEntity, String.class);

        assertEquals(HttpStatus.OK, reportResponse.getStatusCode());
        assertEquals(MediaType.TEXT_HTML, reportResponse.getHeaders().getContentType());
        assertNotNull(reportResponse.getBody());

        String htmlContent = reportResponse.getBody();
        assertTrue(htmlContent.contains("<html"));
        assertTrue(htmlContent.contains("John Smith"));
        assertTrue(htmlContent.contains("1234567890"));
        assertTrue(htmlContent.contains("Salary Deposit"));
    }

    @Test
    void testFullWorkflowIntegrationCsv() throws Exception {
        String reportsUrl = "http://localhost:" + port + "/api/reports";

        ClassPathResource resource = new ClassPathResource("integration-test-data.json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("template", "statement");
        body.add("output", "CSV");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<String> reportResponse = restTemplate.postForEntity(reportsUrl, requestEntity, String.class);

        assertEquals(HttpStatus.OK, reportResponse.getStatusCode());
        assertEquals(MediaType.valueOf("text/csv"), reportResponse.getHeaders().getContentType());
        assertNotNull(reportResponse.getBody());

        String csvContent = reportResponse.getBody();
        assertTrue(csvContent.contains("Account Number"));
        assertTrue(csvContent.contains("Date"));
        assertTrue(csvContent.contains("Description"));
        assertTrue(csvContent.contains("1234567890"));
    }

    @Test
    void testFullWorkflowIntegrationPdf() throws Exception {
        String reportsUrl = "http://localhost:" + port + "/api/reports";

        ClassPathResource resource = new ClassPathResource("integration-test-data.json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("template", "statement");
        body.add("output", "PDF");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> reportResponse = restTemplate.postForEntity(reportsUrl, requestEntity, byte[].class);

        assertEquals(HttpStatus.OK, reportResponse.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, reportResponse.getHeaders().getContentType());
        assertNotNull(reportResponse.getBody());

        byte[] pdfContent = reportResponse.getBody();
        assertTrue(pdfContent.length > 0);

        // Verify PDF header
        String pdfHeader = new String(pdfContent, 0, Math.min(10, pdfContent.length));
        assertTrue(pdfHeader.startsWith("%PDF"));

        // Verify Content-Disposition header
        List<String> contentDisposition = reportResponse.getHeaders().get("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.get(0).contains("attachment"));
        assertTrue(contentDisposition.get(0).contains("filename"));
    }

    @Test
    void testErrorHandlingIntegration() {
        String reportsUrl = "http://localhost:" + port + "/api/reports";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("template", "invalid-template");
        body.add("output", "HTML");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> errorResponse = restTemplate.postForEntity(reportsUrl, requestEntity, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, errorResponse.getStatusCode());
        assertNotNull(errorResponse.getBody());
        assertTrue(errorResponse.getBody().get("error").toString().contains("Bad Request"));
    }

    @Test
    void testHealthCheckIntegration() {
        String healthUrl = "http://localhost:" + port + "/actuator/health";
        ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
    }

    @Test
    void testCompleteDataProcessingPipelineIntegration() throws Exception {
        // This test simulates the complete pipeline from raw data to final report
        String reportsUrl = "http://localhost:" + port + "/api/reports";

        ClassPathResource resource = new ClassPathResource("integration-test-data.json");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Test all three output formats in sequence
        String[] formats = {"HTML", "CSV", "PDF"};
        Class<?>[] responseTypes = {String.class, String.class, byte[].class};
        MediaType[] expectedContentTypes = {MediaType.TEXT_HTML, MediaType.valueOf("text/csv"), MediaType.APPLICATION_PDF};

        for (int i = 0; i < formats.length; i++) {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", resource);
            body.add("template", "statement");
            body.add("output", formats[i]);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<?> reportResponse = restTemplate.postForEntity(reportsUrl, requestEntity, responseTypes[i]);

            assertEquals(HttpStatus.OK, reportResponse.getStatusCode());
            assertEquals(expectedContentTypes[i], reportResponse.getHeaders().getContentType());
            assertNotNull(reportResponse.getBody());

            if (formats[i].equals("PDF")) {
                byte[] content = (byte[]) reportResponse.getBody();
                assertTrue(content.length > 0);
                String pdfHeader = new String(content, 0, Math.min(10, content.length));
                assertTrue(pdfHeader.startsWith("%PDF"));
            } else {
                String content = (String) reportResponse.getBody();
                assertFalse(content.isEmpty());

                if (formats[i].equals("HTML")) {
                    assertTrue(content.contains("<html"));
                    assertTrue(content.contains("John Smith"));
                } else if (formats[i].equals("CSV")) {
                    assertTrue(content.contains("Account Number"));
                    assertTrue(content.contains("1234567890"));
                }
            }
        }
    }

    @Test
    void testReportServiceBusinessLogicIntegration() {
        // Test that the business logic correctly processes the sample data
        Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();

        // Verify statement template exists and supports all formats
        assertTrue(templates.containsKey("statement"));
        List<OutputFormat> statementFormats = templates.get("statement");
        assertEquals(3, statementFormats.size());
        assertTrue(statementFormats.contains(OutputFormat.HTML));
        assertTrue(statementFormats.contains(OutputFormat.CSV));
        assertTrue(statementFormats.contains(OutputFormat.PDF));

        // Verify no other templates are registered (based on current implementation)
        assertEquals(1, templates.size());
    }

    @Test
    void testConcurrentRequestHandlingIntegration() throws Exception {
        String reportsUrl = "http://localhost:" + port + "/api/reports";

        // Test concurrent report generation
        int numberOfThreads = 3;
        Thread[] threads = new Thread[numberOfThreads];
        Exception[] exceptions = new Exception[numberOfThreads];
        ResponseEntity<String>[] responses = new ResponseEntity[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    ClassPathResource resource = new ClassPathResource("integration-test-data.json");

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("file", resource);
                    body.add("template", "statement");
                    body.add("output", "HTML");

                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                    responses[threadIndex] = restTemplate.postForEntity(reportsUrl, requestEntity, String.class);
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(15000); // 15 second timeout
        }

        // Verify all requests succeeded
        for (int i = 0; i < numberOfThreads; i++) {
            if (exceptions[i] != null) {
                throw new AssertionError("Thread " + i + " failed", exceptions[i]);
            }

            assertNotNull(responses[i]);
            assertEquals(HttpStatus.OK, responses[i].getStatusCode());
            assertNotNull(responses[i].getBody());
            assertTrue(responses[i].getBody().contains("John Smith"));
        }
    }
}