package com.wexa.fintrace.controller;


import com.wexa.fintrace.dto.InvestigationGraphResponse;
import com.wexa.fintrace.service.GraphInvestigationService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/investigations")
public class GraphInvestigationController {

    private final GraphInvestigationService graphInvestigationService;

    public GraphInvestigationController(
            GraphInvestigationService graphInvestigationService
    ) {
        this.graphInvestigationService =
                graphInvestigationService;
    }

    @GetMapping("/accounts/{accountId}/graph")
    public InvestigationGraphResponse getGraph(
            @PathVariable String accountId
    ) {

        return graphInvestigationService
                .getInvestigationGraph(accountId);
    }
}
