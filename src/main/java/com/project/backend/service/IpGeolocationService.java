package com.project.backend.service;

import com.project.backend.dto.LocationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for IP-based geolocation
 * Uses ip-api.com free API (no API key required for non-commercial use)
 * Limit: 45 requests per minute from an IP address
 */
@Service
@Slf4j
public class IpGeolocationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String IP_API_URL = "http://ip-api.com/json/";

    // Default fallback location (Belgrade, Serbia)
    private static final double DEFAULT_LAT = 44.7866;
    private static final double DEFAULT_LON = 20.4489;
    private static final String DEFAULT_CITY = "Belgrade";
    private static final String DEFAULT_COUNTRY = "Serbia";

    /**
     * Get approximate location from IP address
     * Falls back to default location (Belgrade) if API fails
     */
    public LocationDTO getLocationFromIp(String ipAddress) {
        log.info("Getting location for IP: {}", ipAddress);

        // Handle localhost/private IPs
        if (isPrivateOrLocalhost(ipAddress)) {
            log.info("Private/localhost IP detected, returning default location");
            return createDefaultLocation(ipAddress);
        }

        try {
            // Call IP geolocation API
            String apiUrl = IP_API_URL + ipAddress;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();

                // Parse JSON response
                JsonNode jsonResponse = objectMapper.readTree(response.toString());

                if ("success".equals(jsonResponse.get("status").asText())) {
                    LocationDTO location = new LocationDTO();
                    location.setLatitude(jsonResponse.get("lat").asDouble());
                    location.setLongitude(jsonResponse.get("lon").asDouble());
                    location.setCity(jsonResponse.get("city").asText());
                    location.setCountry(jsonResponse.get("country").asText());
                    location.setIp(ipAddress);
                    location.setSource("IP_GEOLOCATION");
                    location.setApproximate(true);

                    log.info("Successfully retrieved location: {}, {} ({}, {})",
                            location.getCity(), location.getCountry(),
                            location.getLatitude(), location.getLongitude());

                    return location;
                } else {
                    log.warn("IP API returned non-success status: {}", jsonResponse.get("message").asText());
                }
            } else {
                log.warn("IP API returned non-200 response code: {}", responseCode);
            }

        } catch (Exception e) {
            log.error("Error getting location from IP: {}", e.getMessage(), e);
        }

        // Fallback to default location
        log.info("Falling back to default location");
        return createDefaultLocation(ipAddress);
    }

    /**
     * Create default location (Belgrade)
     */
    private LocationDTO createDefaultLocation(String ipAddress) {
        LocationDTO location = new LocationDTO();
        location.setLatitude(DEFAULT_LAT);
        location.setLongitude(DEFAULT_LON);
        location.setCity(DEFAULT_CITY);
        location.setCountry(DEFAULT_COUNTRY);
        location.setIp(ipAddress);
        location.setSource("DEFAULT_FALLBACK");
        location.setApproximate(true);
        return location;
    }

    /**
     * Check if IP is private or localhost
     */
    private boolean isPrivateOrLocalhost(String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }

        // Localhost patterns
        if (ip.equals("127.0.0.1") ||
            ip.equals("0:0:0:0:0:0:0:1") ||
            ip.equals("::1") ||
            ip.equals("localhost")) {
            return true;
        }

        // Private IP ranges
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            try {
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);

                // 10.0.0.0 - 10.255.255.255
                if (first == 10) return true;

                // 172.16.0.0 - 172.31.255.255
                if (first == 172 && second >= 16 && second <= 31) return true;

                // 192.168.0.0 - 192.168.255.255
                if (first == 192 && second == 168) return true;

            } catch (NumberFormatException e) {
                // Invalid IP format
            }
        }

        return false;
    }
}
