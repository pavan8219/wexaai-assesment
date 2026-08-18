package com.wexa.fintrace.config;

import com.wexa.fintrace.repository.GraphSchemaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    private final GraphSchemaRepository graphSchemaRepository;

    public DatabaseInitializer(GraphSchemaRepository graphSchemaRepository) {
        this.graphSchemaRepository = graphSchemaRepository;
    }

    @Override
    public void run(String... args) {

        System.out.println("Initializing CognoDB schema...");

        graphSchemaRepository.createConstraints();

        System.out.println("CognoDB schema initialized successfully.");
    }
}
