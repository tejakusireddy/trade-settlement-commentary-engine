package com.tsengine.breachdetector.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean("breachesDetectedT2Counter")
    public Counter breachesDetectedT2Counter(MeterRegistry meterRegistry) {
        return Counter.builder("breaches.detected")
                .description("Number of detected settlement breaches")
                .tag("breach_type", "T2")
                .register(meterRegistry);
    }

    @Bean("breachesDetectedT3Counter")
    public Counter breachesDetectedT3Counter(MeterRegistry meterRegistry) {
        return Counter.builder("breaches.detected")
                .description("Number of detected settlement breaches")
                .tag("breach_type", "T3")
                .register(meterRegistry);
    }

    @Bean("breachesDetectedT5Counter")
    public Counter breachesDetectedT5Counter(MeterRegistry meterRegistry) {
        return Counter.builder("breaches.detected")
                .description("Number of detected settlement breaches")
                .tag("breach_type", "T5")
                .register(meterRegistry);
    }

    @Bean("eventsSentToDlqCounter")
    public Counter eventsSentToDlqCounter(MeterRegistry meterRegistry) {
        return Counter.builder("events.sent.to.dlq")
                .description("Number of events sent to dead letter queue")
                .register(meterRegistry);
    }

    @Bean("breachDetectionLatencyTimer")
    public Timer breachDetectionLatencyTimer(MeterRegistry meterRegistry) {
        return Timer.builder("breach.detection.latency")
                .description("Latency for breach detection process")
                .register(meterRegistry);
    }
}
