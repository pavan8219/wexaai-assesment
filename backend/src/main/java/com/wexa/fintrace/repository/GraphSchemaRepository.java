package com.wexa.fintrace.repository;


import org.neo4j.driver.Driver;
import org.springframework.stereotype.Repository;

@Repository
public class GraphSchemaRepository {

    private final Driver driver;

    public GraphSchemaRepository(Driver driver) {
        this.driver = driver;
    }

    public void createConstraints() {

        try (var session = driver.session()) {

            session.executeWrite(tx -> {

                tx.run("""
                    CREATE CONSTRAINT customer_id_unique IF NOT EXISTS
                    FOR (c:Customer)
                    REQUIRE c.customerId IS UNIQUE
                """);

                tx.run("""
                    CREATE CONSTRAINT account_id_unique IF NOT EXISTS
                    FOR (a:BankAccount)
                    REQUIRE a.accountId IS UNIQUE
                """);

                tx.run("""
                    CREATE CONSTRAINT merchant_id_unique IF NOT EXISTS
                    FOR (m:Merchant)
                    REQUIRE m.merchantId IS UNIQUE
                """);

                tx.run("""
                    CREATE CONSTRAINT device_id_unique IF NOT EXISTS
                    FOR (d:Device)
                    REQUIRE d.deviceId IS UNIQUE
                """);

                tx.run("""
                    CREATE CONSTRAINT ip_address_unique IF NOT EXISTS
                    FOR (ip:IPAddress)
                    REQUIRE ip.address IS UNIQUE
                """);

                return null;
            });
        }
    }
}
