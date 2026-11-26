package com.rideshare.locationservice.service;

import com.rideshare.locationservice.dto.DriverLocationRequest;
import com.rideshare.locationservice.dto.NearByDriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private static final String DRIVER_GEO_KEY = "drivers.location";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Update driver location in Redis
     * Called every 3 seconds by drivers phone
     * Map to Redis GEOADD command
     * @param request
     */
    public void updateDriverLocation(DriverLocationRequest request) {
        log.info("Updating location for driver: {}", request.getDriverId());

        Point driverPoint = new Point(
                request.getLongitude(),
                request.getLatitude()
        );

        redisTemplate.opsForGeo().add(DRIVER_GEO_KEY, driverPoint, request.getDriverId());

        log.info("Location updated for driver: {}", request.getDriverId());
    }

    /**
     * Find nearby drivers within given radius
     * Called by Matching Service on ride request
     * Maps to Redis GEORADIUS command
     * @param latitude
     * @param longitude
     * @param radius
     * @return List
     */
    public List<NearByDriverResponse> findNearbyDrivers(double latitude, double longitude, double radius) {
        log.info("Finding drivers near lat: {} long:{} withing {}km", latitude,longitude,radius);

        List<NearByDriverResponse> nearbyDrivers = List.of();

        Circle searchArea = new Circle(
                new Point(longitude, latitude),
                new Distance(radius, Metrics.KILOMETERS)
        );

        GeoResults<RedisGeoCommands.GeoLocation<String>> result = redisTemplate.opsForGeo().radius(
                                                                    DRIVER_GEO_KEY,
                                                                    searchArea,
                                                                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                                                            .includeCoordinates()
                                                                            .includeDistance()
                                                                            .sortAscending()
                                                                            .limit(10)
                                                                    );
        if(result != null) {
            nearbyDrivers =  result.getContent().stream().map(geoResult -> {
                RedisGeoCommands.GeoLocation<String> location = geoResult.getContent();
                return new NearByDriverResponse(location.getName(), location.getPoint().getY(), location.getPoint().getX(), geoResult.getDistance().getValue());
            }).toList();
        }

        log.info("Found {} drivers nearby.", nearbyDrivers.size());

        return nearbyDrivers;
    }

    /**
     * Remove driver when they go offline
     * Maps to Redis ZREM command
     * @param driverId
     */

    public void removeDriver(String driverId) {
        log.info("Removed driver: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVER_GEO_KEY, driverId);
    }
}
