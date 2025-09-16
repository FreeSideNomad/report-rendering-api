package com.tvm.reportrendering.model;

public enum OutputFormat {
    PDF("application/pdf"),
    HTML("text/html"),
    CSV("text/csv");

    private final String mimeType;

    OutputFormat(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }
}