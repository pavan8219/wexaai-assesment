package com.wexa.fintrace.config;

import com.wexa.fintrace.repository.GraphSeedRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class SeedDataInitializer implements CommandLineRunner {

    private final GraphSeedRepository graphSeedRepository;

    public SeedDataInitializer(GraphSeedRepository graphSeedRepository) {
        this.graphSeedRepository = graphSeedRepository;
    }

    @Override
    public void run(String... args) {

        System.out.println("Starting FinTrace seed data...");

        graphSeedRepository.seedCustomers();
        graphSeedRepository.seedAccounts();
        graphSeedRepository.seedMerchants();
        graphSeedRepository.seedDevices();
        graphSeedRepository.seedIpAddresses();
        graphSeedRepository.seedTransactions();
        graphSeedRepository.seedPayments();
        graphSeedRepository.seedAccountSignals();

        System.out.println("FinTrace seed data loaded successfully.");
    }
}
