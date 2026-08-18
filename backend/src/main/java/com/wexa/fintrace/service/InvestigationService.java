package com.wexa.fintrace.service;


import com.wexa.fintrace.dto.SuspiciousNetworkResponse;
import com.wexa.fintrace.repository.InvestigationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InvestigationService {

    private final InvestigationRepository investigationRepository;

    public InvestigationService(
            InvestigationRepository investigationRepository
    ) {
        this.investigationRepository = investigationRepository;
    }

    public List<SuspiciousNetworkResponse> findSuspiciousNetworks(
            String riskLevel,
            long minimumAmount
    ) {
        return investigationRepository.findSuspiciousNetworks(
                riskLevel,
                minimumAmount
        );
    }
}
