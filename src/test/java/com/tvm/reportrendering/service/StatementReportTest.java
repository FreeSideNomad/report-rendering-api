package com.tvm.reportrendering.service;

import com.tvm.reportrendering.reports.statement.StatementModel;
import com.tvm.reportrendering.reports.statement.StatementReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class StatementReportTest {

    private StatementReport statementReport;

    @BeforeEach
    void setUp() {
        statementReport = new StatementReport();
    }

    @Test
    void testParseValidJson() throws IOException {
        InputStream inputStream = new ClassPathResource("sample-statement.json").getInputStream();

        StatementModel result = statementReport.parse(inputStream);

        assertNotNull(result);
        assertEquals(3, result.getAccounts().size());
        assertNotNull(result.getTotalOpeningBalance());
        assertNotNull(result.getTotalClosingBalance());

        // Verify first account
        StatementModel.Account firstAccount = result.getAccounts().get(0);
        assertEquals("John Doe Chequing Account", firstAccount.getAccountName());
        assertEquals("1234567890", firstAccount.getAccountNumber());
        assertEquals(17, firstAccount.getTransactions().size());

        // Verify calculated balances
        assertTrue(firstAccount.getOpeningBalance().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(firstAccount.getClosingBalance().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testParseInvalidJson() {
        // Create a simple invalid JSON stream for testing
        final InputStream inputStream = new java.io.ByteArrayInputStream("{invalid json".getBytes());

        assertThrows(RuntimeException.class, () -> {
            statementReport.parse(inputStream);
        });
    }

    @Test
    void testCalculateAccountBalancesWithEmptyTransactions() throws IOException {
        String jsonContent = """
                {
                    "startDate": "2024-01-01",
                    "endDate": "2024-01-31",
                    "accounts": [
                        {
                            "accountName": "Empty Account",
                            "transitNumber": "00001",
                            "accountNumber": "1111111111",
                            "accountType": "Chequing",
                            "transactions": []
                        }
                    ]
                }
                """;

        InputStream inputStream = new java.io.ByteArrayInputStream(jsonContent.getBytes());
        StatementModel result = statementReport.parse(inputStream);

        StatementModel.Account account = result.getAccounts().get(0);
        assertEquals(BigDecimal.ZERO, account.getOpeningBalance());
        assertEquals(BigDecimal.ZERO, account.getClosingBalance());
    }
}