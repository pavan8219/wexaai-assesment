package com.wexa.fintrace.dto;


import java.util.List;

public record AccountInvestigationResponse(
        String accountId,
        int riskScore,
        String riskLevel,
        List<String> highRiskMerchants,
        List<String> connectedAccounts,
        List<String> sharedDevices,
        List<String> sharedIps,
        List<List<String>> suspiciousNetworks,
        List<String> reasons
) {
}
