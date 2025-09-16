package com.tvm.reportrendering.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportOutputTest {

    @Test
    void testStringContent() {
        String content = "Test content";
        ReportOutput output = new ReportOutput("text/html", content);

        assertEquals("text/html", output.getMimeType());
        assertEquals(content, output.getContent());
        assertFalse(output.isBinary());
        assertEquals(content, output.getContentAsString());
    }

    @Test
    void testBinaryContent() {
        byte[] content = "Binary content".getBytes();
        ReportOutput output = new ReportOutput("application/pdf", content);

        assertEquals("application/pdf", output.getMimeType());
        assertEquals(content, output.getContent());
        assertTrue(output.isBinary());
        assertArrayEquals(content, output.getContentAsByteArray());
    }

    @Test
    void testGetContentAsStringWhenBinary() {
        byte[] content = "Binary content".getBytes();
        ReportOutput output = new ReportOutput("application/pdf", content);

        assertThrows(IllegalStateException.class, () -> {
            output.getContentAsString();
        });
    }

    @Test
    void testGetContentAsByteArrayWhenString() {
        String content = "String content";
        ReportOutput output = new ReportOutput("text/html", content);

        assertThrows(IllegalStateException.class, () -> {
            output.getContentAsByteArray();
        });
    }
}