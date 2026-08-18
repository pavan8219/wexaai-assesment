package com.wexa.fintrace.repository;


import com.wexa.fintrace.dto.GraphLinkResponse;
import com.wexa.fintrace.dto.GraphNodeResponse;
import com.wexa.fintrace.dto.InvestigationGraphResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class GraphInvestigationRepository {

    private final Driver driver;

    public GraphInvestigationRepository(Driver driver) {
        this.driver = driver;
    }

    public InvestigationGraphResponse findInvestigationGraph(
            String accountId
    ) {

        try (Session session = driver.session()) {

            GraphBuilder graph = new GraphBuilder();

            /*
             * 1. Transaction network + merchant
             */
            String transactionQuery = """
                    MATCH path =
                        (account:BankAccount {
                            accountId: $accountId
                        })
                        -[:TRANSFERRED_TO*0..4]->
                        (destination:BankAccount)

                    OPTIONAL MATCH
                        (destination)-[payment:PAYMENT_TO]->(merchant:Merchant)

                    RETURN DISTINCT
                        account,
                        destination,
                        payment,
                        merchant
                    """;

            session.executeRead(tx ->
                    tx.run(
                            transactionQuery,
                            Map.of("accountId", accountId)
                    ).list(record -> {

                        var account = record.get("account");
                        var destination = record.get("destination");

                        // Starting account
                        graph.addNode(
                                account.get("accountId").asString(),
                                account.get("accountId").asString(),
                                "BankAccount",
                                null
                        );

                        // Destination account
                        graph.addNode(
                                destination.get("accountId").asString(),
                                destination.get("accountId").asString(),
                                "BankAccount",
                                null
                        );

                        // Payment → Merchant
                        if (!record.get("merchant").isNull()) {

                            var merchant =
                                    record.get("merchant").asNode();

                            String merchantId =
                                    merchant.get("merchantId").asString();

                            graph.addNode(
                                    merchantId,
                                    merchant.get("name").asString(),
                                    "Merchant",
                                    merchant.get("riskLevel").asString()
                            );

                            var payment =
                                    record.get("payment").asRelationship();

                            graph.addLink(
                                    destination.get("accountId").asString(),
                                    merchantId,
                                    "PAYMENT_TO",
                                    payment.get("amount").asDouble()
                            );
                        }

                        return null;
                    })
            );

            /*
             * 2. Account-to-account transfer relationships
             */
            String transferQuery = """
                    MATCH path =
                        (account:BankAccount {
                            accountId: $accountId
                        })
                        -[:TRANSFERRED_TO*1..4]->
                        (destination:BankAccount)

                    UNWIND relationships(path) AS transfer

                    RETURN DISTINCT
                        startNode(transfer).accountId AS source,
                        endNode(transfer).accountId AS target,
                        transfer.amount AS amount
                    """;

            session.executeRead(tx ->
                    tx.run(
                            transferQuery,
                            Map.of("accountId", accountId)
                    ).list(record -> {

                        String source =
                                record.get("source").asString();

                        String target =
                                record.get("target").asString();

                        graph.addNode(
                                source,
                                source,
                                "BankAccount",
                                null
                        );

                        graph.addNode(
                                target,
                                target,
                                "BankAccount",
                                null
                        );

                        graph.addLink(
                                source,
                                target,
                                "TRANSFERRED_TO",
                                record.get("amount").asDouble()
                        );

                        return null;
                    })
            );

            /*
             * 3. Shared device / IP connections
             */
            String signalQuery = """
                    MATCH
                        (account:BankAccount {
                            accountId: $accountId
                        })
                        -[relationship:USED_DEVICE|USED_IP]->(signal)
                        <-[:USED_DEVICE|USED_IP]-(other:BankAccount)

                    RETURN DISTINCT
                        other,
                        signal,
                        type(relationship) AS relationshipType
                    """;

            session.executeRead(tx ->
                    tx.run(
                            signalQuery,
                            Map.of("accountId", accountId)
                    ).list(record -> {

                        var other = record.get("other").asNode();
                        var signal = record.get("signal").asNode();

                        String otherId =
                                other.get("accountId").asString();

                        graph.addNode(
                                otherId,
                                otherId,
                                "BankAccount",
                                null
                        );

                        String signalId;
                        String signalLabel;
                        String signalType;

                        if (signal.hasLabel("Device")) {

                            signalId =
                                    signal.get("deviceId").asString();

                            signalLabel = signalId;
                            signalType = "Device";

                        } else {

                            signalId =
                                    signal.get("address").asString();

                            signalLabel = signalId;
                            signalType = "IPAddress";
                        }

                        graph.addNode(
                                signalId,
                                signalLabel,
                                signalType,
                                null
                        );

                        graph.addLink(
                                accountId,
                                signalId,
                                record.get("relationshipType").asString(),
                                null
                        );

                        graph.addLink(
                                otherId,
                                signalId,
                                record.get("relationshipType").asString(),
                                null
                        );

                        return null;
                    })
            );

            return graph.build();
        }
    }

    private static class GraphBuilder {

        private final Map<String, GraphNodeResponse> nodes =
                new LinkedHashMap<>();

        private final Set<GraphLinkResponse> links =
                new LinkedHashSet<>();

        void addNode(
                String id,
                String label,
                String type,
                String riskLevel
        ) {

            nodes.putIfAbsent(
                    id,
                    new GraphNodeResponse(
                            id,
                            label,
                            type,
                            riskLevel
                    )
            );
        }

        void addLink(
                String source,
                String target,
                String relationship,
                Double amount
        ) {

            links.add(
                    new GraphLinkResponse(
                            source,
                            target,
                            relationship,
                            amount
                    )
            );
        }

        InvestigationGraphResponse build() {

            return new InvestigationGraphResponse(
                    new ArrayList<>(nodes.values()),
                    new ArrayList<>(links)
            );
        }
    }
}