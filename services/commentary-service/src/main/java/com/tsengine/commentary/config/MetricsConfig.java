package com.tsengine.commentary.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean("commentaryGeneratedAiCounter")
    public Counter commentaryGeneratedAiCounter(MeterRegistry meterRegistry) {
        return Counter.builder("commentary.generated.ai")
                .description("Number of AI-generated commentaries")
                .register(meterRegistry);
    }

    @Bean("commentaryGeneratedTemplateCounter")
    public Counter commentaryGeneratedTemplateCounter(MeterRegistry meterRegistry) {
        return Counter.builder("commentary.generated.template")
                .description("Number of template-generated commentaries")
                .register(meterRegistry);
    }

    @Bean("circuitBreakerOpenCounter")
    public Counter circuitBreakerOpenCounter(MeterRegistry meterRegistry) {
        return Counter.builder("commentary.circuitbreaker.open")
                .description("Count of commentary fallbacks due to circuit/cap")
                .register(meterRegistry);
    }

    @Bean("commentaryGenerationTimer")
    public Timer commentaryGenerationTimer(MeterRegistry meterRegistry) {
        return Timer.builder("commentary.generation.timer")
                .description("Commentary generation latency")
                .register(meterRegistry);
    }
}
