package com.wexa.fintrace.controller;


import com.wexa.fintrace.dto.AccountInvestigationResponse;
import com.wexa.fintrace.service.AccountInvestigationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/investigations")
@CrossOrigin("*")
public class AccountInvestigationController {

    private final AccountInvestigationService accountInvestigationService;

    public AccountInvestigationController(
            AccountInvestigationService accountInvestigationService
    ) {
        this.accountInvestigationService =
                accountInvestigationService;
    }

    @GetMapping("/accounts/{accountId}")
    public AccountInvestigationResponse investigateAccount(
            @PathVariable String accountId
    ) {

        return accountInvestigationService.investigate(accountId);
    }
}
