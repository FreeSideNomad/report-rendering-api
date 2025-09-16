package com.tvm.reportrendering.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tvm.reportrendering.annotation.ReportName;
import com.tvm.reportrendering.model.Account;
import com.tvm.reportrendering.model.StatementModel;
import com.tvm.reportrendering.model.Transaction;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;

@Slf4j
@ReportName("statement")
public class StatementReport extends Report<StatementModel> {

    private final ObjectMapper objectMapper;

    public StatementReport() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected StatementModel parse(InputStream inputStream) {
        log.debug("Parsing statement data from input stream");

        try {
            StatementModel statement = objectMapper.readValue(inputStream, StatementModel.class);

            // Calculate opening and closing balances for each account
            statement.getAccounts().forEach(this::calculateAccountBalances);

            // Calculate total opening and closing balances
            BigDecimal totalOpeningBalance = statement.getAccounts().stream()
                    .map(Account::getOpeningBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalClosingBalance = statement.getAccounts().stream()
                    .map(Account::getClosingBalance)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            statement.setTotalOpeningBalance(totalOpeningBalance);
            statement.setTotalClosingBalance(totalClosingBalance);

            // Sort transactions by date for CSV output
            statement.getAccounts().forEach(account ->
                    account.getTransactions().sort(
                            Comparator.comparing(Transaction::getActionDate)
                                    .thenComparing(Transaction::getValueDate)
                    )
            );

            log.debug("Statement parsed successfully with {} accounts", statement.getAccounts().size());
            return statement;

        } catch (Exception e) {
            log.error("Error parsing statement data: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse statement data", e);
        }
    }

    private void calculateAccountBalances(Account account) {
        if (account.getTransactions().isEmpty()) {
            account.setOpeningBalance(BigDecimal.ZERO);
            account.setClosingBalance(BigDecimal.ZERO);
            return;
        }

        // Sort transactions by date to ensure proper balance calculation
        account.getTransactions().sort(
                Comparator.comparing(Transaction::getActionDate)
                        .thenComparing(Transaction::getValueDate)
        );

        // Set opening balance to the first transaction's balance minus its amount
        Transaction firstTransaction = account.getTransactions().get(0);
        BigDecimal firstAmount = BigDecimal.ZERO;
        if (firstTransaction.getCreditAmount() != null) {
            firstAmount = firstTransaction.getCreditAmount();
        } else if (firstTransaction.getDebitAmount() != null) {
            firstAmount = firstTransaction.getDebitAmount().negate();
        }
        account.setOpeningBalance(firstTransaction.getBalance().subtract(firstAmount));

        // Set closing balance to the last transaction's balance
        Transaction lastTransaction = account.getTransactions().get(account.getTransactions().size() - 1);
        account.setClosingBalance(lastTransaction.getBalance());

        log.debug("Calculated balances for account {}: opening={}, closing={}",
                account.getAccountNumber(), account.getOpeningBalance(), account.getClosingBalance());
    }
}