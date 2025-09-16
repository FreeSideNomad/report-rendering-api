package com.tvm.reportrendering.service;

import com.tvm.reportrendering.annotation.ReportName;
import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import com.tvm.reportrendering.reports.statement.StatementReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.TemplateEngine;
import org.mockito.ArgumentMatchers;
import org.thymeleaf.context.Context;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private Report<?> mockReport;

    @InjectMocks
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitializeReportHandlers() {
        // Create a mock report with annotation
        StatementReport statementReport = new StatementReport();
        Map<String, Object> beans = new HashMap<>();
        beans.put("statementReport", statementReport);

        when(applicationContext.getBeansWithAnnotation(ReportName.class)).thenReturn(beans);

        reportService.initializeReportHandlers();

        Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();
        assertTrue(templates.containsKey("statement"));
    }

    @Test
    void testGenerateReportSuccess() {
        // Setup - use real report instance instead of mock
        MockReport mockReportInstance = new MockReport();
        Map<String, Object> beans = new HashMap<>();
        beans.put("mockReport", mockReportInstance);
        when(applicationContext.getBeansWithAnnotation(ReportName.class)).thenReturn(beans);

        reportService.initializeReportHandlers();

        // Test
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        ReportOutput result = reportService.generateReport(inputStream, "test", OutputFormat.HTML, "en");

        // Verify
        assertNotNull(result);
        assertNotNull(result.getMimeType());
        assertNotNull(result.getContent());
    }

    @Test
    void testGenerateReportWithUnknownTemplate() {
        // Setup
        when(applicationContext.getBeansWithAnnotation(ReportName.class)).thenReturn(new HashMap<>());
        reportService.initializeReportHandlers();

        // Test
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            reportService.generateReport(inputStream, "unknown", OutputFormat.HTML, "en");
        });
    }

    @Test
    void testGetAvailableTemplates() {
        // Setup
        StatementReport statementReport = new StatementReport();
        Map<String, Object> beans = new HashMap<>();
        beans.put("statementReport", statementReport);

        when(applicationContext.getBeansWithAnnotation(ReportName.class)).thenReturn(beans);

        reportService.initializeReportHandlers();

        // Test
        Map<String, List<OutputFormat>> templates = reportService.getAvailableTemplates();

        // Verify
        assertNotNull(templates);
        assertTrue(templates.containsKey("statement"));
        assertEquals(3, templates.get("statement").size()); // PDF, HTML, CSV
    }


    @ReportName("test")
    static class MockReport extends Report<Object> {

        public MockReport() {
            // Initialize with a mock template engine for testing
            this.templateEngine = org.mockito.Mockito.mock(TemplateEngine.class);
            this.pdfService = org.mockito.Mockito.mock(PdfService.class);

            // Setup mock behavior
            org.mockito.Mockito.when(templateEngine.process(ArgumentMatchers.any(String.class),
                ArgumentMatchers.any(Context.class)))
                .thenReturn("<html>Mock Content</html>");
        }

        @Override
        protected Object parse(InputStream inputStream) {
            return new Object();
        }
    }
}