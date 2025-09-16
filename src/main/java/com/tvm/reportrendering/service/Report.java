package com.tvm.reportrendering.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.InputStream;
import java.util.Map;

@Slf4j
public abstract class Report<T> {

    @Autowired
    protected TemplateEngine templateEngine;

    @Autowired
    protected PdfService pdfService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportOutput process(InputStream inputStream, String templateName, OutputFormat outputFormat, String language) {
        log.info("Processing report with template: {}, format: {} and language: {}", templateName, outputFormat, language);

        try {
            T model = parse(inputStream);
            log.debug("Parsed model successfully");

            Map<String, String> labels = loadLanguageLabels(templateName, language);
            log.debug("Loaded language labels for language: {}", language);

            ReportOutput output = render(model, templateName, outputFormat, labels);
            log.info("Report processed successfully");
            return output;
        } catch (Exception e) {
            log.error("Error processing report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process report: " + e.getMessage(), e);
        }
    }

    private Map<String, String> loadLanguageLabels(String templateName, String language) {
        String languageFileName = String.format("language_%s.json", language);
        String languageFilePath = String.format("templates/%s/%s", templateName, languageFileName);

        try {
            ClassPathResource resource = new ClassPathResource(languageFilePath);
            if (!resource.exists()) {
                throw new IllegalArgumentException("Language file not found: " + languageFilePath);
            }

            log.debug("Loading language file: {}", languageFilePath);
            return objectMapper.readValue(resource.getInputStream(), Map.class);
        } catch (Exception e) {
            log.error("Error loading language file {}: {}", languageFilePath, e.getMessage());
            throw new RuntimeException("Failed to load language file: " + languageFilePath, e);
        }
    }

    protected abstract T parse(InputStream inputStream);

    protected ReportOutput render(T model, String templateName, OutputFormat outputFormat, Map<String, String> labels) {
        Context context = new Context();
        context.setVariable("model", model);
        context.setVariable("labels", labels);

        String templatePath = String.format("%s/%s", templateName, outputFormat.name().toLowerCase());

        switch (outputFormat) {
            case HTML:
                return renderHtml(context, templatePath);
            case CSV:
                return renderCsv(context, templatePath);
            case PDF:
                return renderPdf(context, templateName);
            default:
                throw new IllegalArgumentException("Unsupported output format: " + outputFormat);
        }
    }

    private ReportOutput renderHtml(Context context, String templatePath) {
        String content = templateEngine.process(templatePath, context);
        return new ReportOutput(OutputFormat.HTML.getMimeType(), content);
    }

    private ReportOutput renderCsv(Context context, String templatePath) {
        String content = templateEngine.process(templatePath, context);
        return new ReportOutput(OutputFormat.CSV.getMimeType(), content);
    }

    private ReportOutput renderPdf(Context context, String templateName) {
        String htmlContent = templateEngine.process(templateName + "/pdf", context);

        // Check for header and footer templates
        String headerContent = null;
        String footerContent = null;

        try {
            headerContent = templateEngine.process(templateName + "/pdf_header", context);
        } catch (Exception e) {
            log.debug("No header template found for {}", templateName);
        }

        try {
            footerContent = templateEngine.process(templateName + "/pdf_footer", context);
        } catch (Exception e) {
            log.debug("No footer template found for {}", templateName);
        }

        byte[] pdfContent = pdfService.generatePdf(htmlContent, headerContent, footerContent);
        return new ReportOutput(OutputFormat.PDF.getMimeType(), pdfContent);
    }
}