package com.tvm.reportrendering.reports.statement;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class StatementModel {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<Account> accounts;
    private BigDecimal totalOpeningBalance;
    private BigDecimal totalClosingBalance;

    @Data
    public static class Account {
        private String accountName;
        private String transitNumber;
        private String accountNumber;
        private String accountType;
        private BigDecimal openingBalance;
        private BigDecimal closingBalance;
        private List<Transaction> transactions;
    }

    @Data
    public static class Transaction {
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
}