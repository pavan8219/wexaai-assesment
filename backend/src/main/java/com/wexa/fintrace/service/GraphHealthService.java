package com.wexa.fintrace.service;

import com.wexa.fintrace.repository.GraphRepository;
import org.springframework.stereotype.Service;

@Service
public class GraphHealthService {
    private final GraphRepository graphRepository;

    public GraphHealthService(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    public String checkConnection() {
        return graphRepository.testConnection();
    }
}
