package com.wexa.fintrace.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Repository;

@Repository
public class GraphRepository {
    private  final Driver driver;

    public GraphRepository(Driver driver) {
        this.driver = driver;
    }

    public String testConnection() {

        try (Session session = driver.session()) {

            return session.executeRead(tx ->
                    tx.run("RETURN 'CognoDB Connected' AS message")
                            .single()
                            .get("message")
                            .asString()
            );
        }
    }
}
