package com.wexa.fintrace.service;



import com.wexa.fintrace.dto.AccountInvestigationResponse;
import com.wexa.fintrace.dto.SharedSignalResponse;
import com.wexa.fintrace.dto.SuspiciousNetworkResponse;
import com.wexa.fintrace.repository.AccountNetworkRepository;
import com.wexa.fintrace.repository.InvestigationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountInvestigationService {

    private final InvestigationRepository investigationRepository;
    private final AccountNetworkRepository accountNetworkRepository;

    public AccountInvestigationService(
            InvestigationRepository investigationRepository,
            AccountNetworkRepository accountNetworkRepository
    ) {
        this.investigationRepository = investigationRepository;
        this.accountNetworkRepository = accountNetworkRepository;
    }

    public AccountInvestigationResponse investigate(
            String accountId
    ) {

        // 1. Find suspicious transaction networks
        List<SuspiciousNetworkResponse> networks =
                investigationRepository.findSuspiciousNetworksForAccount(
                        accountId,
                        "HIGH",
                        50000
                );

        // 2. Find shared devices / IPs
        List<SharedSignalResponse> signals =
                accountNetworkRepository.findSharedSignalsForAccount(
                        accountId
                );

        // 3. Extract high-risk merchants
        List<String> highRiskMerchants =
                networks.stream()
                        .map(SuspiciousNetworkResponse::merchant)
                        .distinct()
                        .toList();

        // 4. Extract connected accounts
        List<String> connectedAccounts =
                signals.stream()
                        .map(signal -> signal.account1().equals(accountId)
                                ? signal.account2()
                                : signal.account1())
                        .distinct()
                        .toList();

        // 5. Extract devices
        List<String> sharedDevices =
                signals.stream()
                        .filter(signal ->
                                signal.signalType().equals("Device"))
                        .map(SharedSignalResponse::sharedSignal)
                        .distinct()
                        .toList();

        // 6. Extract IP addresses
        List<String> sharedIps =
                signals.stream()
                        .filter(signal ->
                                signal.signalType().equals("IPAddress"))
                        .map(SharedSignalResponse::sharedSignal)
                        .distinct()
                        .toList();

        // 7. Extract networks
        List<List<String>> suspiciousNetworks =
                networks.stream()
                        .map(SuspiciousNetworkResponse::network)
                        .toList();

        // 8. Calculate risk
        int riskScore = 0;

        List<String> reasons = new ArrayList<>();

        if (!highRiskMerchants.isEmpty()) {
            riskScore += 40;
            reasons.add("Connected to a high-risk merchant");
        }

        boolean hasLongTransactionNetwork =
                networks.stream()
                        .anyMatch(network -> network.hops() >= 3);

        if (hasLongTransactionNetwork) {
            riskScore += 25;
            reasons.add("Detected a multi-hop transaction network");
        }

        if (!sharedDevices.isEmpty()) {
            riskScore += 20;
            reasons.add("Shares a device with another account");
        }

        if (!sharedIps.isEmpty()) {
            riskScore += 15;
            reasons.add("Shares an IP address with another account");
        }

        String riskLevel = calculateRiskLevel(riskScore);

        return new AccountInvestigationResponse(
                accountId,
                riskScore,
                riskLevel,
                highRiskMerchants,
                connectedAccounts,
                sharedDevices,
                sharedIps,
                suspiciousNetworks,
                reasons
        );
    }

    private String calculateRiskLevel(int score) {

        if (score >= 80) {
            return "CRITICAL";
        }

        if (score >= 60) {
            return "HIGH";
        }

        if (score >= 30) {
            return "MEDIUM";
        }

        return "LOW";
    }
}
