package com.wexa.fintrace.service;


import com.wexa.fintrace.dto.InvestigationGraphResponse;
import com.wexa.fintrace.repository.GraphInvestigationRepository;
import org.springframework.stereotype.Service;

@Service
public class GraphInvestigationService {

    private final GraphInvestigationRepository graphInvestigationRepository;

    public GraphInvestigationService(
            GraphInvestigationRepository graphInvestigationRepository
    ) {
        this.graphInvestigationRepository =
                graphInvestigationRepository;
    }

    public InvestigationGraphResponse getInvestigationGraph(
            String accountId
    ) {

        return graphInvestigationRepository
                .findInvestigationGraph(accountId);
    }
}
