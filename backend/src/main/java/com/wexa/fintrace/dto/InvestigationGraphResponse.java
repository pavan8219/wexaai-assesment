package com.wexa.fintrace.dto;


import java.util.List;


public record InvestigationGraphResponse(
        List<GraphNodeResponse> nodes,
        List<GraphLinkResponse> links
) {
}
