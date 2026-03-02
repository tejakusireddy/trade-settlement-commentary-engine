package com.tsengine.commentary.config;

import com.tsengine.schema.CommentaryCompleted;
import com.tsengine.schema.CommentaryApproved;
import com.tsengine.schema.DlqEvent;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaProducerConfig {

    private final Environment environment;

    public KafkaProducerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public ProducerFactory<String, CommentaryCompleted> commentaryCompletedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigMap());
    }

    @Bean
    public ProducerFactory<String, CommentaryApproved> commentaryApprovedProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigMap());
    }

    @Bean
    public ProducerFactory<String, DlqEvent> dlqProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigMap());
    }

    @Bean
    public KafkaTemplate<String, CommentaryCompleted> commentaryCompletedKafkaTemplate() {
        return new KafkaTemplate<>(commentaryCompletedProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, CommentaryApproved> commentaryApprovedKafkaTemplate() {
        return new KafkaTemplate<>(commentaryApprovedProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, DlqEvent> commentaryDlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    private Map<String, Object> producerConfigMap() {
        Map<String, Object> config = new HashMap<>();
        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.bootstrap-servers", "localhost:9092")
        );
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        config.put(
                "schema.registry.url",
                environment.getProperty("spring.kafka.schema-registry-url", "http://localhost:8081")
        );
        return config;
    }
}
