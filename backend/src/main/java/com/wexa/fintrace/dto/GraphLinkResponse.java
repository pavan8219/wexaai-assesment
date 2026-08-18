package com.wexa.fintrace.dto;


public record GraphLinkResponse(
        String source,
        String target,
        String relationship,
        Double amount
) {
}
