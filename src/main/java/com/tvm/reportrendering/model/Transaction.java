package com.tvm.reportrendering.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Transaction {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate actionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate valueDate;

    private String transactionType;
    private String description;
    private BigDecimal creditAmount;
    private BigDecimal debitAmount;
    private BigDecimal balance;
}