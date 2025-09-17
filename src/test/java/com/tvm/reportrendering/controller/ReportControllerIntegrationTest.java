package com.tvm.reportrendering.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.nio.file.Files;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAvailableTemplatesIntegration() throws Exception {
        mockMvc.perform(get("/templates"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.statement").exists())
                .andExpect(jsonPath("$.statement").isArray())
                .andExpect(jsonPath("$.statement", hasSize(3)))
                .andExpect(jsonPath("$.statement", containsInAnyOrder("HTML", "CSV", "PDF")));
    }

    @Test
    void testGenerateHtmlReportIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "HTML")
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"))
                .andExpect(content().string(containsString("<html")))
                .andExpect(content().string(containsString("John Smith")))
                .andExpect(content().string(containsString("1234567890")))
                .andExpect(content().string(containsString("Salary Deposit")))
                .andExpect(content().string(containsString("5,000.00")));
    }

    @Test
    void testGenerateCsvReportIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "CSV")
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string(containsString("Account Number")))
                .andExpect(content().string(containsString("Date")))
                .andExpect(content().string(containsString("Description")))
                .andExpect(content().string(containsString("Amount")))
                .andExpect(content().string(containsString("1234567890")))
                .andExpect(content().string(containsString("Salary Deposit")));
    }

    @Test
    void testGeneratePdfReportIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "PDF")
                        .param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"statement-report.pdf\""));
    }

    @Test
    void testGenerateReportWithInvalidTemplateIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "invalid-template")
                        .param("output", "HTML")
                        .param("language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").value("Template not allowed: invalid-template"));
    }

    @Test
    void testGenerateReportWithInvalidOutputFormatIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "INVALID")
                        .param("language", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenerateReportWithMissingFileIntegration() throws Exception {
        mockMvc.perform(multipart("/reports")
                        .param("template", "statement")
                        .param("output", "HTML")
                        .param("language", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenerateReportWithMissingTemplateIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("output", "HTML")
                        .param("language", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenerateReportWithMissingOutputIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-test-data.json",
                "application/json",
                fileContent
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("language", "en"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenerateReportWithEmptyFileIntegration() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.json",
                "application/json",
                new byte[0]
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "HTML")
                        .param("language", "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").value("Report generation failed"));
    }

    @Test
    void testGenerateReportWithInvalidJsonFileIntegration() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid.json",
                "application/json",
                "{invalid json}".getBytes()
        );

        mockMvc.perform(multipart("/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "HTML")
                        .param("language", "en"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").value("Report generation failed"));
    }

    @Test
    void testGenerateReportWithAllFormatsIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        String[] formats = {"HTML", "CSV", "PDF"};
        String[] expectedContentTypes = {"text/html", "text/csv", "application/pdf"};

        for (int i = 0; i < formats.length; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "integration-test-data.json",
                    "application/json",
                    fileContent
            );

            mockMvc.perform(multipart("/reports")
                            .file(file)
                            .param("template", "statement")
                            .param("output", formats[i])
                            .param("language", "en"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(expectedContentTypes[i]));
        }
    }

    @Test
    void testConcurrentReportGenerationIntegration() throws Exception {
        ClassPathResource resource = new ClassPathResource("integration-test-data.json");
        byte[] fileContent = Files.readAllBytes(resource.getFile().toPath());

        // Test concurrent requests
        Thread[] threads = new Thread[5];
        Exception[] exceptions = new Exception[5];

        for (int i = 0; i < 5; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    MockMultipartFile file = new MockMultipartFile(
                            "file",
                            "integration-test-data.json",
                            "application/json",
                            fileContent
                    );

                    mockMvc.perform(multipart("/reports")
                                    .file(file)
                                    .param("template", "statement")
                                    .param("output", "HTML")
                        .param("language", "en"))
                            .andExpect(status().isOk())
                            .andExpect(content().contentType("text/html"));
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
            thread.join(10000); // 10 second timeout
        }

        // Check for exceptions
        for (int i = 0; i < exceptions.length; i++) {
            if (exceptions[i] != null) {
                throw new AssertionError("Thread " + i + " failed", exceptions[i]);
            }
        }
    }
}