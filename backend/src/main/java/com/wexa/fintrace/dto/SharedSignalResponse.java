package com.wexa.fintrace.dto;


public record SharedSignalResponse(
        String account1,
        String account2,
        String signalType,
        String sharedSignal
) {
}
