package com.wexa.fintrace.dto;


import java.util.List;

public record SuspiciousNetworkResponse(
        String sourceAccount,
        List<String> network,
        String merchant,
        String riskLevel,
        int hops
) {
}
