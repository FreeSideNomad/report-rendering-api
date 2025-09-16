package com.tvm.reportrendering.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Account {
    private String accountName;
    private String transitNumber;
    private String accountNumber;
    private String accountType;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private List<Transaction> transactions;
}