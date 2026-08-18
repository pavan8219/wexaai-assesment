package com.wexa.fintrace.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class GraphSeedRepository {

    private final Driver driver;

    public GraphSeedRepository(Driver driver) {
        this.driver = driver;
    }

    public void seedCustomers() {

        List<Map<String, Object>> customers = List.of(
                Map.of(
                        "customerId", "C001",
                        "name", "Rahul Sharma",
                        "email", "rahul@example.com",
                        "phone", "9000000001"
                ),
                Map.of(
                        "customerId", "C002",
                        "name", "Anil Kumar",
                        "email", "anil@example.com",
                        "phone", "9000000002"
                ),
                Map.of(
                        "customerId", "C003",
                        "name", "Priya Reddy",
                        "email", "priya@example.com",
                        "phone", "9000000003"
                ),
                Map.of(
                        "customerId", "C004",
                        "name", "Kiran Rao",
                        "email", "kiran@example.com",
                        "phone", "9000000004"
                ),
                Map.of(
                        "customerId", "C005",
                        "name", "Suresh Kumar",
                        "email", "suresh@example.com",
                        "phone", "9000000005"
                ),
                Map.of(
                        "customerId", "C006",
                        "name", "Neha Patel",
                        "email", "neha@example.com",
                        "phone", "9000000006"
                ),
                Map.of(
                        "customerId", "C007",
                        "name", "Vikram Singh",
                        "email", "vikram@example.com",
                        "phone", "9000000007"
                ),
                Map.of(
                        "customerId", "C008",
                        "name", "Meena Das",
                        "email", "meena@example.com",
                        "phone", "9000000008"
                ),
                Map.of(
                        "customerId", "C009",
                        "name", "Arjun Verma",
                        "email", "arjun@example.com",
                        "phone", "9000000009"
                ),
                Map.of(
                        "customerId", "C010",
                        "name", "Sneha Rao",
                        "email", "sneha@example.com",
                        "phone", "9000000010"
                )
        );

        String query = """
                UNWIND $customers AS row

                MERGE (c:Customer {
                    customerId: row.customerId
                })

                SET c.name = row.name,
                    c.email = row.email,
                    c.phone = row.phone
                """;

        execute(query, customers, "customers");
    }

    public void seedAccounts() {

        List<Map<String, Object>> accounts = List.of(
                account("A001", "100000000001", "Global Bank", "SAVINGS", "ACTIVE", "C001"),
                account("A002", "100000000002", "Global Bank", "SAVINGS", "ACTIVE", "C002"),
                account("A003", "100000000003", "Global Bank", "CURRENT", "ACTIVE", "C003"),
                account("A004", "100000000004", "Global Bank", "SAVINGS", "ACTIVE", "C004"),
                account("A005", "100000000005", "Global Bank", "SAVINGS", "ACTIVE", "C005"),
                account("A006", "100000000006", "National Bank", "SAVINGS", "ACTIVE", "C006"),
                account("A007", "100000000007", "National Bank", "CURRENT", "ACTIVE", "C007"),
                account("A008", "100000000008", "National Bank", "SAVINGS", "ACTIVE", "C008"),
                account("A009", "100000000009", "City Bank", "SAVINGS", "ACTIVE", "C009"),
                account("A010", "100000000010", "City Bank", "CURRENT", "ACTIVE", "C010")
        );

        String query = """
                UNWIND $accounts AS row

                MERGE (a:BankAccount {
                    accountId: row.accountId
                })

                SET a.accountNumber = row.accountNumber,
                    a.bankName = row.bankName,
                    a.accountType = row.accountType,
                    a.status = row.status

                WITH a, row

                MATCH (c:Customer {
                    customerId: row.customerId
                })

                MERGE (c)-[:OWNS]->(a)
                """;

        execute(query, accounts, "accounts");
    }

    public void seedMerchants() {

        List<Map<String, Object>> merchants = List.of(
                merchant("M001", "Amazon", "E_COMMERCE", "LOW", "IN"),
                merchant("M002", "QuickMart", "RETAIL", "LOW", "IN"),
                merchant("M003", "LuckyPlay Gaming", "ONLINE_GAMING", "HIGH", "IN"),
                merchant("M004", "BetZone", "ONLINE_GAMBLING", "HIGH", "IN"),
                merchant("M005", "CryptoFast", "CRYPTO", "MEDIUM", "IN")
        );

        String query = """
                UNWIND $merchants AS row

                MERGE (m:Merchant {
                    merchantId: row.merchantId
                })

                SET m.name = row.name,
                    m.category = row.category,
                    m.riskLevel = row.riskLevel,
                    m.country = row.country
                """;

        execute(query, merchants, "merchants");
    }

    public void seedDevices() {

        List<Map<String, Object>> devices = List.of(
                device("D001", "MOBILE", "Android"),
                device("D002", "MOBILE", "Android"),
                device("D003", "MOBILE", "iOS"),
                device("D004", "DESKTOP", "Windows"),
                device("D005", "MOBILE", "Android"),
                device("D006", "DESKTOP", "Windows")
        );

        String query = """
                UNWIND $devices AS row

                MERGE (d:Device {
                    deviceId: row.deviceId
                })

                SET d.deviceType = row.deviceType,
                    d.os = row.os
                """;

        execute(query, devices, "devices");
    }

    public void seedIpAddresses() {

        List<Map<String, Object>> ips = List.of(
                Map.of("address", "103.21.45.10", "country", "IN"),
                Map.of("address", "103.21.45.11", "country", "IN"),
                Map.of("address", "103.21.45.12", "country", "IN"),
                Map.of("address", "103.21.45.13", "country", "IN"),
                Map.of("address", "185.22.91.20", "country", "IN"),
                Map.of("address", "185.22.91.21", "country", "IN")
        );

        String query = """
                UNWIND $ips AS row

                MERGE (ip:IPAddress {
                    address: row.address
                })

                SET ip.country = row.country
                """;

        execute(query, ips, "ips");
    }

    private void execute(
            String query,
            List<Map<String, Object>> rows,
            String parameterName
    ) {

        try (Session session = driver.session()) {

            session.executeWrite(tx -> {
                tx.run(query, Map.of(parameterName, rows));
                return null;
            });
        }
    }

    private Map<String, Object> account(
            String accountId,
            String accountNumber,
            String bankName,
            String accountType,
            String status,
            String customerId
    ) {
        return Map.of(
                "accountId", accountId,
                "accountNumber", accountNumber,
                "bankName", bankName,
                "accountType", accountType,
                "status", status,
                "customerId", customerId
        );
    }

    private Map<String, Object> merchant(
            String merchantId,
            String name,
            String category,
            String riskLevel,
            String country
    ) {
        return Map.of(
                "merchantId", merchantId,
                "name", name,
                "category", category,
                "riskLevel", riskLevel,
                "country", country
        );
    }

    private Map<String, Object> device(
            String deviceId,
            String deviceType,
            String os
    ) {
        return Map.of(
                "deviceId", deviceId,
                "deviceType", deviceType,
                "os", os
        );
    }

    private Map<String, Object> transfer(
            String transactionId,
            String sourceAccount,
            String destinationAccount,
            long amount,
            String timestamp,
            String channel
    ) {
        return Map.of(
                "transactionId", transactionId,
                "sourceAccount", sourceAccount,
                "destinationAccount", destinationAccount,
                "amount", amount,
                "timestamp", timestamp,
                "channel", channel
        );
    }

    public void seedTransactions() {

        List<Map<String, Object>> transfers = List.of(

                // Suspicious network #1
                transfer("T001", "A001", "A002", 80000, "2026-08-10T10:15:00", "UPI"),
                transfer("T002", "A002", "A003", 75000, "2026-08-10T11:20:00", "UPI"),
                transfer("T003", "A003", "A004", 70000, "2026-08-10T12:10:00", "IMPS"),

                // Suspicious network #2
                transfer("T004", "A006", "A007", 90000, "2026-08-11T09:15:00", "NEFT"),
                transfer("T005", "A007", "A008", 85000, "2026-08-11T10:30:00", "NEFT"),
                transfer("T006", "A008", "A009", 80000, "2026-08-11T11:45:00", "IMPS"),

                // Normal transactions
                transfer("T007", "A001", "A005", 2500, "2026-08-12T09:00:00", "UPI"),
                transfer("T008", "A005", "A010", 3000, "2026-08-12T10:00:00", "UPI"),
                transfer("T009", "A004", "A006", 5000, "2026-08-12T11:00:00", "NEFT"),
                transfer("T010", "A009", "A010", 1500, "2026-08-12T12:00:00", "UPI")
        );

        String query = """
        UNWIND $transfers AS row

        MATCH (source:BankAccount {
            accountId: row.sourceAccount
        })

        MATCH (destination:BankAccount {
            accountId: row.destinationAccount
        })

        MERGE (source)-[
            t:TRANSFERRED_TO {
                transactionId: row.transactionId
            }
        ]->(destination)

        SET t.amount = row.amount,
            t.timestamp = datetime(row.timestamp),
            t.channel = row.channel,
            t.status = 'SUCCESS'
        """;

        execute(query, transfers, "transfers");
    }

    public void seedPayments() {

        List<Map<String, Object>> payments = List.of(

                // Suspicious network #1
                payment("P001", "A004", "M003", 65000, "2026-08-10T13:00:00"),
                payment("P002", "A003", "M003", 60000, "2026-08-10T13:30:00"),

                // Suspicious network #2
                payment("P003", "A009", "M004", 70000, "2026-08-11T12:30:00"),
                payment("P004", "A008", "M004", 65000, "2026-08-11T13:00:00"),

                // Normal activity
                payment("P005", "A001", "M001", 2000, "2026-08-12T14:00:00"),
                payment("P006", "A002", "M002", 1500, "2026-08-12T14:30:00"),
                payment("P007", "A005", "M001", 3000, "2026-08-12T15:00:00"),
                payment("P008", "A010", "M002", 1200, "2026-08-12T15:30:00"),
                payment("P009", "A006", "M005", 10000, "2026-08-12T16:00:00")
        );

        String query = """
        UNWIND $payments AS row

        MATCH (a:BankAccount {
            accountId: row.accountId
        })

        MATCH (m:Merchant {
            merchantId: row.merchantId
        })

        MERGE (a)-[
            p:PAYMENT_TO {
                transactionId: row.transactionId
            }
        ]->(m)

        SET p.amount = row.amount,
            p.timestamp = datetime(row.timestamp),
            p.channel = 'ONLINE',
            p.status = 'SUCCESS'
        """;

        execute(query, payments, "payments");
    }
    private Map<String, Object> payment(
            String transactionId,
            String accountId,
            String merchantId,
            long amount,
            String timestamp
    ) {
        return Map.of(
                "transactionId", transactionId,
                "accountId", accountId,
                "merchantId", merchantId,
                "amount", amount,
                "timestamp", timestamp
        );
    }

    public void seedAccountSignals() {

        List<Map<String, Object>> signals = List.of(

                Map.of("accountId", "A001", "deviceId", "D001", "ip", "103.21.45.10"),
                Map.of("accountId", "A002", "deviceId", "D001", "ip", "103.21.45.10"),
                Map.of("accountId", "A003", "deviceId", "D002", "ip", "103.21.45.11"),
                Map.of("accountId", "A004", "deviceId", "D002", "ip", "103.21.45.11"),

                Map.of("accountId", "A006", "deviceId", "D004", "ip", "185.22.91.20"),
                Map.of("accountId", "A007", "deviceId", "D004", "ip", "185.22.91.20"),
                Map.of("accountId", "A008", "deviceId", "D005", "ip", "185.22.91.21"),
                Map.of("accountId", "A009", "deviceId", "D005", "ip", "185.22.91.21"),

                Map.of("accountId", "A005", "deviceId", "D003", "ip", "103.21.45.12"),
                Map.of("accountId", "A010", "deviceId", "D006", "ip", "103.21.45.13")
        );

        String query = """
            UNWIND $signals AS row

            MATCH (a:BankAccount {
                accountId: row.accountId
            })

            MATCH (d:Device {
                deviceId: row.deviceId
            })

            MATCH (ip:IPAddress {
                address: row.ip
            })

            MERGE (a)-[:USED_DEVICE]->(d)

            MERGE (a)-[:USED_IP]->(ip)
            """;

        execute(query, signals, "signals");
    }
}