package com.rideshare.rideservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideRequest {
    @NotBlank(message = "Ride id is required")
    private String riderId;

    @NotBlank(message = "Driver id is required")
    private String driverId;

    @NotBlank(message = "Pickup latitude is required")
    private double pickupLatitude;

    @NotBlank(message = "Pickup longitude is required")
    private double pickupLongitude;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotBlank(message = "Drop latitude is required")
    private double dropLatitude;

    @NotBlank(message = "Drop longitude is required")
    private double dropLongitude;

    @NotBlank(message = "Drop address is required")
    private String dropAddress;
}
