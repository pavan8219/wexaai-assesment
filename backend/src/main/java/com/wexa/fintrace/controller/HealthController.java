package com.wexa.fintrace.controller;

import com.wexa.fintrace.service.GraphHealthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class HealthController {
    private final GraphHealthService graphHealthService;

    public HealthController(GraphHealthService graphHealthService) {
        this.graphHealthService = graphHealthService;
    }

    @GetMapping("/api/health/graph")
    public String checkGraphConnection() {
        return graphHealthService.checkConnection();
    }
}
