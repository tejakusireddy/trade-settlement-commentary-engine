package com.tsengine.gateway.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean("gatewayRequestCounter")
    public Counter gatewayRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.requests")
                .description("Gateway request count")
                .tag("method", "unknown")
                .tag("path", "unknown")
                .tag("status", "unknown")
                .register(meterRegistry);
    }

    @Bean("gatewayRateLimitCounter")
    public Counter gatewayRateLimitCounter(MeterRegistry meterRegistry) {
        return Counter.builder("gateway.ratelimit.hits")
                .description("Gateway rate limit hits")
                .tag("userId", "unknown")
                .register(meterRegistry);
    }

    @Bean("gatewayLatencyTimer")
    public Timer gatewayLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("gateway.latency")
                .description("Gateway request latency")
                .register(meterRegistry);
    }
}
