package com.tvm.reportrendering.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PdfService {

    public byte[] generatePdf(String htmlContent, String headerContent, String footerContent) {
        log.debug("Generating PDF from HTML content");

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();

            // Set up resource routing for shared template resources
            page.route("**", route -> {
                String url = route.request().url();
                log.debug("Playwright route request: {}", url);

                if (url.contains("/resources/")) {
                    try {
                        String resourcePath = url.substring(url.indexOf("/resources/") + 1); // Keep "resources/..."
                        log.debug("Attempting to serve resource: {}", resourcePath);

                        // Map to templates/resources directory
                        String fullPath = "templates/" + resourcePath;
                        ClassPathResource resource = new ClassPathResource(fullPath);

                        if (resource.exists()) {
                            byte[] resourceData = resource.getInputStream().readAllBytes();
                            String contentType = getContentType(resourcePath);

                            route.fulfill(new Route.FulfillOptions()
                                    .setStatus(200)
                                    .setContentType(contentType)
                                    .setBodyBytes(resourceData));

                            log.info("Successfully served resource: {} (size: {} bytes)", fullPath, resourceData.length);
                        } else {
                            log.warn("Resource not found: {}", fullPath);
                            route.fulfill(new Route.FulfillOptions().setStatus(404));
                        }
                    } catch (Exception e) {
                        log.error("Error handling resource route: {}", e.getMessage(), e);
                        route.fulfill(new Route.FulfillOptions().setStatus(500));
                    }
                } else {
                    // Continue with normal request
                    route.resume();
                }
            });

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

    private String getContentType(String resourcePath) {
        String lowercasePath = resourcePath.toLowerCase();
        if (lowercasePath.endsWith(".png")) {
            return "image/png";
        } else if (lowercasePath.endsWith(".jpg") || lowercasePath.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowercasePath.endsWith(".gif")) {
            return "image/gif";
        } else if (lowercasePath.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowercasePath.endsWith(".css")) {
            return "text/css";
        } else if (lowercasePath.endsWith(".js")) {
            return "application/javascript";
        } else {
            return "application/octet-stream";
        }
    }
}