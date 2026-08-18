package com.wexa.fintrace.dto;

public record GraphNodeResponse(
        String id,
        String label,
        String type,
        String riskLevel
) {
}
