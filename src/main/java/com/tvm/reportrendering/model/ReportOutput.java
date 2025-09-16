package com.tvm.reportrendering.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReportOutput {
    private String mimeType;
    private Object content; // String for text formats, byte[] for binary

    public boolean isBinary() {
        return content instanceof byte[];
    }

    public String getContentAsString() {
        if (content instanceof String) {
            return (String) content;
        }
        throw new IllegalStateException("Content is not a string");
    }

    public byte[] getContentAsByteArray() {
        if (content instanceof byte[]) {
            return (byte[]) content;
        }
        throw new IllegalStateException("Content is not a byte array");
    }
}