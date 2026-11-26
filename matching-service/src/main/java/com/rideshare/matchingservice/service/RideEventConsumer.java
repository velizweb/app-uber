package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideEventConsumer {

    private final MatchingService matchingService;

    /**
     * Listens to ride.requested kafka topic
     * Triggered every time Ride Service published a new ride request
     *
     * FLOW:
     * Ride Service -> kafka (ride.requested) -> this consumer -> MatchingService
     */
    @KafkaListener(topics = "ride.requested", groupId = "matching-service-group")
    public void consumerRideRequestedEvent(RideRequestedEvent event) {
        try {
            matchingService.matchingDriverForRide(event);
        } catch (Exception e) {
            log.error("Error processing ride request: {} - {}", event.getRideId(), e.getMessage());
            //In production send to dead letter queue for retry
        }
    }
}
