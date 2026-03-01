package com.tsengine.breachdetector.config;

import com.tsengine.schema.BreachEvent;
import com.tsengine.schema.DlqEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.schema-registry-url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, BreachEvent> breachEventProducerFactory() {
        Map<String, Object> configs = producerConfigs();
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public ProducerFactory<String, DlqEvent> dlqEventProducerFactory() {
        Map<String, Object> configs = producerConfigs();
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public KafkaTemplate<String, BreachEvent> breachEventKafkaTemplate() {
        return new KafkaTemplate<>(breachEventProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, DlqEvent> dlqEventKafkaTemplate() {
        return new KafkaTemplate<>(dlqEventProducerFactory());
    }

    private Map<String, Object> producerConfigs() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ProducerConfig.ACKS_CONFIG, "all");
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configs.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        configs.put("schema.registry.url", schemaRegistryUrl);
        return configs;
    }
}
