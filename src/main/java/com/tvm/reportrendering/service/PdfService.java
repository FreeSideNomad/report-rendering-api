package com.tvm.reportrendering.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfService {

    public byte[] generatePdf(String htmlContent, String headerContent, String footerContent) {
        log.debug("Generating PDF from HTML content");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            page.setContent(htmlContent);

            Page.PdfOptions pdfOptions = new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true);

            if (headerContent != null) {
                pdfOptions.setHeaderTemplate(headerContent);
                pdfOptions.setDisplayHeaderFooter(true);
            }

            if (footerContent != null) {
                pdfOptions.setFooterTemplate(footerContent);
                pdfOptions.setDisplayHeaderFooter(true);
            }

            byte[] pdfBytes = page.pdf(pdfOptions);
            browser.close();

            log.debug("PDF generated successfully, size: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }
}