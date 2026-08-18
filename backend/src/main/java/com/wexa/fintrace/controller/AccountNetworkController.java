package com.wexa.fintrace.controller;


import com.wexa.fintrace.dto.SharedSignalResponse;
import com.wexa.fintrace.service.AccountNetworkService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin("*")
public class AccountNetworkController {

    private final AccountNetworkService accountNetworkService;

    public AccountNetworkController(
            AccountNetworkService accountNetworkService
    ) {
        this.accountNetworkService = accountNetworkService;
    }

    @GetMapping("/shared-signals")
    public List<SharedSignalResponse> findSharedSignals() {
        return accountNetworkService.findSharedSignals();
    }
}
