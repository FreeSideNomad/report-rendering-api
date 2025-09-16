package com.tvm.reportrendering.controller;

import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import com.tvm.reportrendering.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportService reportService;

    @Test
    void testGenerateReportHtml() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", "{}".getBytes());
        ReportOutput reportOutput = new ReportOutput("text/html", "<html>Test Report</html>");

        when(reportService.generateReport(any(), eq("statement"), eq(OutputFormat.HTML)))
                .thenReturn(reportOutput);

        mockMvc.perform(multipart("/api/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "HTML"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/html"))
                .andExpect(content().string("<html>Test Report</html>"));
    }

    @Test
    void testGenerateReportPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", "{}".getBytes());
        byte[] pdfContent = "PDF content".getBytes();
        ReportOutput reportOutput = new ReportOutput("application/pdf", pdfContent);

        when(reportService.generateReport(any(), eq("statement"), eq(OutputFormat.PDF)))
                .thenReturn(reportOutput);

        mockMvc.perform(multipart("/api/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "PDF"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"statement-report.pdf\""));
    }

    @Test
    void testGenerateReportCsv() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", "{}".getBytes());
        ReportOutput reportOutput = new ReportOutput("text/csv", "header1,header2\nvalue1,value2");

        when(reportService.generateReport(any(), eq("statement"), eq(OutputFormat.CSV)))
                .thenReturn(reportOutput);

        mockMvc.perform(multipart("/api/reports")
                        .file(file)
                        .param("template", "statement")
                        .param("output", "CSV"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string("header1,header2\nvalue1,value2"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"statement-report.csv\""));
    }

    @Test
    void testGenerateReportWithInvalidTemplate() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.json", "application/json", "{}".getBytes());

        when(reportService.generateReport(any(), eq("invalid"), eq(OutputFormat.HTML)))
                .thenThrow(new IllegalArgumentException("No report handler found for template: invalid"));

        mockMvc.perform(multipart("/api/reports")
                        .file(file)
                        .param("template", "invalid")
                        .param("output", "HTML"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("No report handler found for template: invalid"));
    }

    @Test
    void testGetAvailableTemplates() throws Exception {
        Map<String, List<OutputFormat>> templates = new HashMap<>();
        templates.put("statement", List.of(OutputFormat.HTML, OutputFormat.PDF, OutputFormat.CSV));

        when(reportService.getAvailableTemplates()).thenReturn(templates);

        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statement").isArray())
                .andExpect(jsonPath("$.statement.length()").value(3));
    }

    @Test
    void testGetAvailableTemplatesWithError() throws Exception {
        when(reportService.getAvailableTemplates()).thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(get("/api/templates"))
                .andExpect(status().isInternalServerError());
    }
}