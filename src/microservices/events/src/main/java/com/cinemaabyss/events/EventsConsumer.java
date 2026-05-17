package com.cinemaabyss.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventsConsumer.class);

    @KafkaListener(topics = "movie-events", groupId = "events-service")
    public void onMovieEvent(String message) {
        log.info("Received from movie-events: {}", message);
    }

    @KafkaListener(topics = "user-events", groupId = "events-service")
    public void onUserEvent(String message) {
        log.info("Received from user-events: {}", message);
    }

    @KafkaListener(topics = "payment-events", groupId = "events-service")
    public void onPaymentEvent(String message) {
        log.info("Received from payment-events: {}", message);
    }
}