package com.rideshare.rideservice.service;

import com.rideshare.rideservice.dto.RideRequest;
import com.rideshare.rideservice.dto.RideResponse;
import com.rideshare.rideservice.enums.RideStatus;
import com.rideshare.rideservice.event.RideRequestEvent;
import com.rideshare.rideservice.mapper.RideMapper;
import com.rideshare.rideservice.model.Ride;
import com.rideshare.rideservice.repository.RideRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;

    private final KafkaTemplate<String, RideRequestEvent> kafkaTemplate;
    private static final String RIDE_REQUESTED_TOPIC = "ride.requested";

    public RideResponse requestRide(RideRequest request) {
        log.info("new ride request from rider: {}", request.getRiderId());

        // Step 1 - save ride to database
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupAddress(request.getPickupAddress());
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimateFare(calculateEstimateFare(request));

        Ride saved = rideRepository.save(ride);

        // Step 2 - Publish event to kafka
        // Matching service will consume this and find nearest driver
        RideRequestEvent event = new RideRequestEvent(
                                    saved.getId(),
                                    saved.getRiderId(),
                                    saved.getPickupLatitude(),
                                    saved.getPickupLongitude(),
                                    saved.getPickupAddress(),
                                    saved.getDropLatitude(),
                                    saved.getDropLongitude(),
                                    saved.getDropAddress()
                                );

        kafkaTemplate.send(RIDE_REQUESTED_TOPIC, saved.getId(), event);
        log.info("RideRequestedEvent published to kafka for rider: {}", saved.getId());

        // Update status to matching
        saved.setStatus(RideStatus.MATCHING);
        rideRepository.save(saved);
        return RideMapper.toResponse(saved);
    }

    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        return RideMapper.toResponse(ride);
    }

    public List<RideResponse> getRideByRider(String riderId) {
        return rideRepository.findByRiderIdOrderByCompletedAtDesc(riderId)
                .stream()
                .map(RideMapper::toResponse)
                .toList();
    }

    public RideResponse startRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if(ride.getStatus() != RideStatus.ACCEPTED){
            throw new RuntimeException("Ride cannot be started. Current status: "+ ride.getStatus());
        }

        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);

        return RideMapper.toResponse(ride);
    }

    public RideResponse completedRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        if(ride.getStatus() != RideStatus.RIDE_STARTED){
            throw new RuntimeException("Ride cannot be completed. Current status: "+ ride.getStatus());
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setEstimateFare(ride.getEstimateFare());
        rideRepository.save(ride);

        return RideMapper.toResponse(ride);
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);

        return RideMapper.toResponse(ride);
    }

    private double calculateEstimateFare(RideRequest request) {
        // Simplified Haversine distance calculation

        double lat1 = Math.toRadians(request.getPickupLatitude());
        double lat2 = Math.toRadians(request.getDropLatitude());

        double lon1 = Math.toRadians(request.getPickupLongitude());
        double lon2 = Math.toRadians(request.getDropLongitude());

        double dLat = lat2 -lat1;
        double dLon = lon2 - lon1;

        // 1. Tu expresión (parámetro 'a')
        // Corresponde al cálculo del parámetro a dentro de la Fórmula de Haversine,
        // que sirve para calcular la distancia real entre dos coordenadas GPS
        // sobre la superficie curva de la Tierra (y no en línea recta como en un mapa plano).
        double a = Math.pow(Math.sin(dLat/2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dLon / 2), 2);

        // 2. Convierte 'a' en el ángulo central entre los dos puntos (en radianes)
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 3. Multiplica el ángulo por el radio medio de la Tierra (6371 km)
        BigDecimal distanceKm = new BigDecimal(6371.0 * c);

        BigDecimal limit = new BigDecimal("2.5"); // Limit KM
        BigDecimal baseFare = new BigDecimal("3"); // Base Fare
        BigDecimal ratePerKm = new BigDecimal("0.5"); // KM additional

        // if distancia max > limit entonces = 1
        // Recalculamos la tarifa con los Km adicionales
        // Sabiendo que el km base es de 2.5
        BigDecimal extraKm = distanceKm.compareTo(limit) > 0
                ? distanceKm.subtract(limit)
                : BigDecimal.ZERO;

        BigDecimal totalFare = extraKm
                .multiply(ratePerKm)
                .add(baseFare)
                .setScale(2, RoundingMode.HALF_UP); // Resultado exacto: 4.75

        return totalFare.doubleValue();

    }

    public void updateRideWithDriver(String rideId, String driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));

        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);
    }
}
