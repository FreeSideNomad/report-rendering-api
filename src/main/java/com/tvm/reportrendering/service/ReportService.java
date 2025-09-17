package com.tvm.reportrendering.service;

import com.tvm.reportrendering.annotation.ReportName;
import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ReportService {

    @Autowired
    private ApplicationContext applicationContext;

    private Map<String, Report<?>> reportHandlers = new HashMap<>();

    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        return input.replace('\r', '_').replace('\n', '_').replace('\t', '_');
    }

    @PostConstruct
    public void initializeReportHandlers() {
        log.info("Initializing report handlers");

        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(ReportName.class);

        for (Object bean : beans.values()) {
            if (bean instanceof Report) {
                ReportName annotation = bean.getClass().getAnnotation(ReportName.class);
                String templateName = annotation.value();
                reportHandlers.put(templateName, (Report<?>) bean);
                log.info("Registered report handler for template: {}", templateName);
            }
        }

        log.info("Initialized {} report handlers", reportHandlers.size());
    }

    public ReportOutput generateReport(InputStream inputStream, String templateName, OutputFormat outputFormat, String language) {
        log.info("Generating report for template: {} with format: {} and language: {}", sanitizeForLogging(templateName), outputFormat, sanitizeForLogging(language));

        Report<?> handler = reportHandlers.get(templateName);
        if (handler == null) {
            log.error("No report handler found for template: {}", sanitizeForLogging(templateName));
            throw new IllegalArgumentException("No report handler found for template: " + templateName);
        }

        return handler.process(inputStream, templateName, outputFormat, language);
    }

    public Map<String, List<OutputFormat>> getAvailableTemplates() {
        Map<String, List<OutputFormat>> templates = new HashMap<>();

        for (String templateName : reportHandlers.keySet()) {
            // For now, all templates support all formats
            // This could be enhanced to check actual template availability
            templates.put(templateName, Arrays.asList(OutputFormat.values()));
        }

        log.debug("Available templates: {}", templates);
        return templates;
    }
}