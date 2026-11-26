package com.rideshare.rideservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event publish to kafka topic: ride.matched
 * Consumed by Ride Service to update ride with assigned driver
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideMatchedEvent {
    private String riderId;
    private String rideId;
    private String driverId;
    private double driverLatitude;
    private double driverLongitude;
    private double distanceToPickupKm;
}
