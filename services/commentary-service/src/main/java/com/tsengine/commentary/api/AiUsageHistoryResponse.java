package com.tsengine.commentary.api;

import java.util.List;

public record AiUsageHistoryResponse(
        List<AiUsageDailyPointResponse> daily,
        List<AiUsageCallResponse> recentCalls
) {
}
