package com.wexa.fintrace.controller;


import com.wexa.fintrace.dto.SuspiciousNetworkResponse;
import com.wexa.fintrace.service.InvestigationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investigations")
@CrossOrigin("*")
public class InvestigationController {

    private final InvestigationService investigationService;

    public InvestigationController(
            InvestigationService investigationService
    ) {
        this.investigationService = investigationService;
    }

    @GetMapping("/suspicious-networks")
    public List<SuspiciousNetworkResponse> findSuspiciousNetworks(
            @RequestParam(defaultValue = "HIGH") String riskLevel,
            @RequestParam(defaultValue = "50000") long minimumAmount
    ) {

        return investigationService.findSuspiciousNetworks(
                riskLevel,
                minimumAmount
        );
    }
}

