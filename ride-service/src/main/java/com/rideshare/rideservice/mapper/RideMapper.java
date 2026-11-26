package com.rideshare.rideservice.mapper;

import com.rideshare.rideservice.dto.RideResponse;
import com.rideshare.rideservice.model.Ride;

public class RideMapper {
    public static RideResponse toResponse(Ride ride) {
        return new RideResponse(
                ride.getId(),
                ride.getRiderId(),
                ride.getDriverId(),
                ride.getPickupLatitude(),
                ride.getPickupLongitude(),
                ride.getPickupAddress(),
                ride.getDropLatitude(),
                ride.getDropLongitude(),
                ride.getDropAddress(),
                ride.getStatus(),
                ride.getEstimateFare(),
                ride.getActualFare(),
                ride.getCreateAt(),
                ride.getUpdatedAt(),
                ride.getStartedAt(),
                ride.getCompletedAt()
        );
    }
}
