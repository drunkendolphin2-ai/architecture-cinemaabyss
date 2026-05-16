package com.cinemaabyss.events;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    private static final Logger log = LoggerFactory.getLogger(EventsController.class);

    public static final String TOPIC_MOVIE = "movie-events";
    public static final String TOPIC_USER = "user-events";
    public static final String TOPIC_PAYMENT = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public EventsController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", true);
    }

    @PostMapping("/movie")
    public ResponseEntity<Map<String, String>> movieEvent(@RequestBody JsonNode payload) {
        return publish(TOPIC_MOVIE, payload);
    }

    @PostMapping("/user")
    public ResponseEntity<Map<String, String>> userEvent(@RequestBody JsonNode payload) {
        return publish(TOPIC_USER, payload);
    }

    @PostMapping("/payment")
    public ResponseEntity<Map<String, String>> paymentEvent(@RequestBody JsonNode payload) {
        return publish(TOPIC_PAYMENT, payload);
    }

    private ResponseEntity<Map<String, String>> publish(String topic, JsonNode payload) {
        String message = payload.toString();
        kafkaTemplate.send(topic, message);
        log.info("Published to topic {}: {}", topic, message);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("status", "success"));
    }
}