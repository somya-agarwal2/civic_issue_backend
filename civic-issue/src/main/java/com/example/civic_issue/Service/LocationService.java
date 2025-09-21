package com.example.civic_issue.Service;

import com.example.civic_issue.dto.LocationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;

@Service
public class LocationService {

    @Value("${locationiq.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Convenience method to get address directly from latitude and longitude.
     */
    public String getAddressFromCoordinates(double latitude, double longitude) {
        LocationRequest request = new LocationRequest();
        request.setLatitude(latitude);
        request.setLongitude(longitude);
        return getAddressFromCoordinates(request);
    }

    /**
     * Fetch human-readable address using LocationIQ reverse geocoding.
     */
    public String getAddressFromCoordinates(LocationRequest request) {
        try {
            String url = "https://us1.locationiq.com/v1/reverse.php?key=" + apiKey +
                    "&lat=" + request.getLatitude() +
                    "&lon=" + request.getLongitude() +
                    "&format=json";

            String response = restTemplate.getForObject(url, String.class);

            if (response == null || response.isBlank()) {
                return "Unknown location";
            }

            JSONObject json = new JSONObject(response);
            return json.optString("display_name", "Unknown location");

        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown location";
        }
    }
}
