package com.tsengine.tradeingest.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsengine.tradeingest.domain.TradeRepository;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TradeIngestIntegrationTest {

    private static final String TOPIC = "trade.events";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("trade_settlement")
            .withUsername("trade_user")
            .withPassword("trade_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", () -> "mock://trade-ingest-integration");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("app.kafka.topics.trade-events", () -> TOPIC);
    }

    @BeforeAll
    void createTopic() throws Exception {
        try (AdminClient adminClient = AdminClient.create(Map.of("bootstrap.servers", kafka.getBootstrapServers()))) {
            adminClient.createTopics(Collections.singleton(new NewTopic(TOPIC, 3, (short) 1))).all().get();
        } catch (Exception ex) {
            // Topic may already exist from prior test run.
        }
    }

    @Test
    void shouldIngestTradePersistAndPublishEvent() throws Exception {
        String payload = """
                {
                  "tradeId":"TRD-IT-1",
                  "instrument":"AAPL",
                  "tradeDate":"2026-02-28",
                  "expectedSettlementDate":"2026-03-03",
                  "counterparty":"CPTY-IT",
                  "quantity": "100.000000",
                  "price": "125.500000",
                  "currency":"USD",
                  "idempotencyKey":"it-key-1"
                }
                """;

        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tradeId").value("TRD-IT-1"));

        assertThat(tradeRepository.findByTradeId("TRD-IT-1")).isPresent();
        assertThat(readPublishedEventKey()).isEqualTo("TRD-IT-1");
    }

    @Test
    void shouldReturnConflictForDuplicateIdempotencyKey() throws Exception {
        String payload = """
                {
                  "tradeId":"TRD-IT-2",
                  "instrument":"MSFT",
                  "tradeDate":"2026-02-28",
                  "expectedSettlementDate":"2026-03-03",
                  "counterparty":"CPTY-IT",
                  "quantity": "50.000000",
                  "price": "99.500000",
                  "currency":"USD",
                  "idempotencyKey":"it-key-2"
                }
                """;

        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldGetTradeById() throws Exception {
        String payload = """
                {
                  "tradeId":"TRD-IT-3",
                  "instrument":"GOOG",
                  "tradeDate":"2026-02-28",
                  "expectedSettlementDate":"2026-03-03",
                  "counterparty":"CPTY-IT",
                  "quantity": "30.000000",
                  "price": "200.000000",
                  "currency":"USD",
                  "idempotencyKey":"it-key-3"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID id = UUID.fromString(body.path("data").path("id").asText());

        mockMvc.perform(get("/api/v1/trades/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tradeId").value("TRD-IT-3"));
    }

    private String readPublishedEventKey() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "trade-ingest-it-" + System.nanoTime());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());

        try (Consumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(TOPIC));
            long deadline = System.currentTimeMillis() + 10_000L;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, byte[]> record : records) {
                    return record.key();
                }
            }
        }
        return null;
    }
}
