package com.wexa.fintrace.repository;

import com.wexa.fintrace.dto.SuspiciousNetworkResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class InvestigationRepository {

    private final Driver driver;

    public InvestigationRepository(Driver driver) {
        this.driver = driver;
    }

    public List<SuspiciousNetworkResponse> findSuspiciousNetworks(
            String riskLevel,
            long minimumAmount
    ) {

        String query = """
                MATCH path =
                    (source:BankAccount)
                    -[:TRANSFERRED_TO*2..4]->
                    (destination:BankAccount)
                    -[:PAYMENT_TO]->
                    (merchant:Merchant)

                WHERE merchant.riskLevel = $riskLevel
                  AND ALL(
                      relationship IN relationships(path)
                      WHERE relationship.amount >= $minimumAmount
                  )

                RETURN DISTINCT
                    source.accountId AS sourceAccount,

                    [node IN nodes(path) |
                        CASE
                            WHEN node:BankAccount THEN node.accountId
                            WHEN node:Merchant THEN node.merchantId
                        END
                    ] AS network,

                    merchant.name AS merchant,
                    merchant.riskLevel AS riskLevel,
                    length(path) AS hops

                ORDER BY hops DESC
                """;

        try (Session session = driver.session()) {

            return session.executeRead(tx ->
                    tx.run(
                            query,
                            Map.of(
                                    "riskLevel", riskLevel,
                                    "minimumAmount", minimumAmount
                            )
                    ).list(record -> new SuspiciousNetworkResponse(
                            record.get("sourceAccount").asString(),
                            record.get("network").asList(value -> value.asString()),
                            record.get("merchant").asString(),
                            record.get("riskLevel").asString(),
                            record.get("hops").asInt()
                    ))
            );
        }
    }

    public List<SuspiciousNetworkResponse> findSuspiciousNetworksForAccount(
            String accountId,
            String riskLevel,
            long minimumAmount
    ) {

        String query = """
            MATCH path =
                (source:BankAccount {
                    accountId: $accountId
                })
                -[:TRANSFERRED_TO*1..4]->
                (destination:BankAccount)
                -[:PAYMENT_TO]->
                (merchant:Merchant)

            WHERE merchant.riskLevel = $riskLevel
              AND ALL(
                  relationship IN relationships(path)
                  WHERE relationship.amount >= $minimumAmount
              )

            RETURN DISTINCT
                source.accountId AS sourceAccount,

                [node IN nodes(path) |
                    CASE
                        WHEN node:BankAccount THEN node.accountId
                        WHEN node:Merchant THEN node.merchantId
                    END
                ] AS network,

                merchant.name AS merchant,
                merchant.riskLevel AS riskLevel,
                length(path) AS hops

            ORDER BY hops DESC
            """;

        try (Session session = driver.session()) {

            return session.executeRead(tx ->
                    tx.run(
                            query,
                            Map.of(
                                    "accountId", accountId,
                                    "riskLevel", riskLevel,
                                    "minimumAmount", minimumAmount
                            )
                    ).list(record -> new SuspiciousNetworkResponse(
                            record.get("sourceAccount").asString(),
                            record.get("network")
                                    .asList(value -> value.asString()),
                            record.get("merchant").asString(),
                            record.get("riskLevel").asString(),
                            record.get("hops").asInt()
                    ))
            );
        }
    }
}