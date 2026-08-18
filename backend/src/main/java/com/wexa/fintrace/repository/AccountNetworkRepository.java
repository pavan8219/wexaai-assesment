package com.wexa.fintrace.repository;

import com.wexa.fintrace.dto.SharedSignalResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AccountNetworkRepository {

    private final Driver driver;

    public AccountNetworkRepository(Driver driver) {
        this.driver = driver;
    }

    public List<SharedSignalResponse> findSharedSignals() {

        String query = """
                MATCH
                    (a1:BankAccount)-[:USED_DEVICE|USED_IP]->(signal)
                    <-[:USED_DEVICE|USED_IP]-(a2:BankAccount)

                WHERE a1.accountId < a2.accountId

                RETURN DISTINCT
                    a1.accountId AS account1,
                    a2.accountId AS account2,

                    CASE
                        WHEN signal:Device THEN 'Device'
                        WHEN signal:IPAddress THEN 'IPAddress'
                    END AS signalType,

                    CASE
                        WHEN signal:Device THEN signal.deviceId
                        WHEN signal:IPAddress THEN signal.address
                    END AS sharedSignal

                ORDER BY account1, account2
                """;

        try (Session session = driver.session()) {

            return session.executeRead(tx ->
                    tx.run(query)
                            .list(record -> new SharedSignalResponse(
                                    record.get("account1").asString(),
                                    record.get("account2").asString(),
                                    record.get("signalType").asString(),
                                    record.get("sharedSignal").asString()
                            ))
            );
        }
    }

    public List<SharedSignalResponse> findSharedSignalsForAccount(
            String accountId
    ) {

        String query = """
            MATCH
                (account:BankAccount {
                    accountId: $accountId
                })
                -[:USED_DEVICE|USED_IP]->(signal)
                <-[:USED_DEVICE|USED_IP]-(other:BankAccount)

            WHERE other.accountId <> account.accountId

            RETURN DISTINCT
                account.accountId AS account1,
                other.accountId AS account2,

                CASE
                    WHEN signal:Device THEN 'Device'
                    WHEN signal:IPAddress THEN 'IPAddress'
                END AS signalType,

                CASE
                    WHEN signal:Device THEN signal.deviceId
                    WHEN signal:IPAddress THEN signal.address
                END AS sharedSignal

            ORDER BY other.accountId
            """;

        try (Session session = driver.session()) {

            return session.executeRead(tx ->
                    tx.run(
                            query,
                            Map.of("accountId", accountId)
                    ).list(record -> new SharedSignalResponse(
                            record.get("account1").asString(),
                            record.get("account2").asString(),
                            record.get("signalType").asString(),
                            record.get("sharedSignal").asString()
                    ))
            );
        }
    }
}
