package com.tvm.reportrendering.model;

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
}