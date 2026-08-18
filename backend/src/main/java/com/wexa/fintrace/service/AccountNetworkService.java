package com.wexa.fintrace.service;


import com.wexa.fintrace.dto.SharedSignalResponse;
import com.wexa.fintrace.repository.AccountNetworkRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountNetworkService {

    private final AccountNetworkRepository accountNetworkRepository;

    public AccountNetworkService(
            AccountNetworkRepository accountNetworkRepository
    ) {
        this.accountNetworkRepository = accountNetworkRepository;
    }

    public List<SharedSignalResponse> findSharedSignals() {
        return accountNetworkRepository.findSharedSignals();
    }
    public List<SharedSignalResponse> findSharedSignalsForAccount(
            String accountId
    ) {
        return accountNetworkRepository.findSharedSignalsForAccount(accountId);
    }
}
