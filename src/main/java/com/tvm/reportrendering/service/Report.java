package com.tvm.reportrendering.service;

import com.tvm.reportrendering.model.OutputFormat;
import com.tvm.reportrendering.model.ReportOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.InputStream;

@Slf4j
public abstract class Report<T> {

    @Autowired
    protected TemplateEngine templateEngine;

    @Autowired
    protected PdfService pdfService;

    public ReportOutput process(InputStream inputStream, String templateName, OutputFormat outputFormat) {
        log.info("Processing report with template: {} and format: {}", templateName, outputFormat);

        try {
            T model = parse(inputStream);
            log.debug("Parsed model successfully");

            ReportOutput output = render(model, templateName, outputFormat);
            log.info("Report processed successfully");
            return output;
        } catch (Exception e) {
            log.error("Error processing report: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process report: " + e.getMessage(), e);
        }
    }

    protected abstract T parse(InputStream inputStream);

    protected ReportOutput render(T model, String templateName, OutputFormat outputFormat) {
        Context context = new Context();
        context.setVariable("model", model);

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