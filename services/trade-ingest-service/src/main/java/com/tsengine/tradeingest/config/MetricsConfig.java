package com.tsengine.tradeingest.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean("tradesIngestedSuccessCounter")
    public Counter tradesIngestedSuccessCounter(MeterRegistry meterRegistry) {
        return Counter.builder("trades.ingested")
                .description("Number of trades processed by ingest service")
                .tag("status", "success")
                .register(meterRegistry);
    }

    @Bean("tradesIngestedDuplicateCounter")
    public Counter tradesIngestedDuplicateCounter(MeterRegistry meterRegistry) {
        return Counter.builder("trades.ingested")
                .description("Number of trades processed by ingest service")
                .tag("status", "duplicate")
                .register(meterRegistry);
    }

    @Bean("tradesIngestedFailedCounter")
    public Counter tradesIngestedFailedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("trades.ingested")
                .description("Number of trades processed by ingest service")
                .tag("status", "failed")
                .register(meterRegistry);
    }

    @Bean("tradeIngestLatencyTimer")
    public Timer tradeIngestLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("trade.ingest.latency")
                .description("Latency of trade ingest operation")
                .register(meterRegistry);
    }
}
