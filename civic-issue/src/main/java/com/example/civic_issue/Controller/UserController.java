package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.Service.LocationService;
import com.example.civic_issue.dto.LocationRequest;
import com.example.civic_issue.dto.LocationResponse;
import com.example.civic_issue.enums.Role;
import com.example.civic_issue.repo.UserRepository;
import com.example.civic_issue.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;


@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final LocationService locationService;

    @PostMapping("/update-location")
    public ResponseEntity<LocationResponse> updateLocation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LocationRequest locationRequest) {

        String token = authHeader.substring(7);
        String phoneNumber = jwtUtil.extractPhoneNumber(token);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if(user.getRole() != Role.CITIZEN){
            return ResponseEntity.status(403).body(null);
        }

        // Update lat/lng
        user.setLatitude(locationRequest.getLatitude());
        user.setLongitude(locationRequest.getLongitude());

        // Get human-readable address
        String address = locationService.getAddressFromCoordinates(
                locationRequest.getLatitude(),
                locationRequest.getLongitude()
        );
        user.setAddress(address);
        user.setLastLocationUpdate(LocalDateTime.now());

        userRepository.save(user);

        // Return DTO instead of string
        return ResponseEntity.ok(
                new LocationResponse(
                        user.getLatitude(),
                        user.getLongitude(),
                        user.getAddress(),
                        user.getLastLocationUpdate()
                )
        );
    }

    @GetMapping("/my-location")
    public ResponseEntity<LocationResponse> getMyLocation(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String phoneNumber = jwtUtil.extractPhoneNumber(token);

        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(
                new LocationResponse(
                        user.getLatitude(),
                        user.getLongitude(),
                        user.getAddress(),
                        user.getLastLocationUpdate()
                )
        );
    }
}
