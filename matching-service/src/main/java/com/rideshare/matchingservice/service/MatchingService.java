package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.client.LocationServiceClient;
import com.rideshare.matchingservice.dto.NearByDriverResponse;
import com.rideshare.matchingservice.event.RideMatchedEvent;
import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final LocationServiceClient locationServiceClient;
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.mached";
    private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;

    /**
     * Main matching
     * Called when RideRequestedEvent is consumed from kafka
     * @param event
     *
     * STEPS:
     * 1. Ask location Service for nearby drivers
     */

    public void matchingDriverForRide(RideRequestedEvent event) {
        List<NearByDriverResponse> nearByDrivers = locationServiceClient.getNearByDrivers(
                event.getPickupLatitude(),
                event.getPickupLongitude(),
                DEFAULT_SEARCH_RADIUS_KM
        );

        if(nearByDrivers.isEmpty()) {
            log.warn("No drivers found near ride");
            return;
        }

        // Step 2: Score each driver and pick the best one
        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearByDrivers);

        if(bestDriver.isEmpty()) {
            log.warn("Could not find suitable driver for ride");
        }

        NearByDriverResponse assignedDriver = bestDriver.get();

        // STEP 3: Publish RideMatchedEvent to kafka
        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm()
        );

        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
        log.info("RideMatchedEvent published");
    }

    /**
     * Driver Sourcing algorithm
     *
     * Distance 70%
     * Rating 20%
     * Score = (1/distance) * distanceWeight + rating * ratingWeight
     * @param nearByDrivers
     * @return
     */
    private Optional<NearByDriverResponse> findBestDriver(List<NearByDriverResponse> nearByDrivers) {
        double distanceWeight = 0.7;
        double ratingWeight = 0.3;

        return nearByDrivers.stream()
                .max(Comparator.comparingDouble(driver -> {
                    // Distance score: closer * higher score
                    //add 0.1 to avoid division by zero
                    double distanceScore = 1.0/(driver.getDistanceInKm() + 0.1);

                    // Simulated rating between 4.0 and 5.0
                    // In production: fetch from Driver Service
                    double simulationRating = 4.0 + Math.random();

                    // Final weighted score
                    return (distanceScore * distanceWeight) + (simulationRating + ratingWeight);
                }));
    }
}
