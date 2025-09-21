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


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final LocationService locationService;

    // ================== UPDATE LOCATION ==================
    @PostMapping("/update-location")
    public ResponseEntity<LocationResponse> updateLocation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody LocationRequest locationRequest) {

        User user = getCitizenFromAuthHeader(authHeader);

        if(locationRequest.getLatitude() == null || locationRequest.getLongitude() == null){
            return ResponseEntity.badRequest().body(null);
        }

        user.setLatitude(locationRequest.getLatitude());
        user.setLongitude(locationRequest.getLongitude());

        // Resolve human-readable address safely
        String address = locationService.getAddressFromCoordinates(
                locationRequest.getLatitude(),
                locationRequest.getLongitude()
        );
        user.setAddress(address != null ? address : "Unknown location");
        user.setLastLocationUpdate(LocalDateTime.now());

        userRepository.save(user);

        return ResponseEntity.ok(
                new LocationResponse(
                        user.getLatitude(),
                        user.getLongitude(),
                        user.getAddress(),
                        user.getLastLocationUpdate()
                )
        );
    }

    // ================== GET MY LOCATION ==================
    @GetMapping("/my-location")
    public ResponseEntity<LocationResponse> getMyLocation(@RequestHeader("Authorization") String authHeader) {
        User user = getCitizenFromAuthHeader(authHeader);

        return ResponseEntity.ok(
                new LocationResponse(
                        user.getLatitude(),
                        user.getLongitude(),
                        user.getAddress(),
                        user.getLastLocationUpdate()
                )
        );
    }

    // ================== HELPER: GET CITIZEN FROM TOKEN ==================
    private User getCitizenFromAuthHeader(String authHeader){
        User user = getUserFromAuthHeader(authHeader);

        if(user.getRole() != Role.CITIZEN){
            throw new RuntimeException("Only citizens can access this endpoint");
        }
        return user;
    }

    // ================== HELPER: GET USER FROM TOKEN ==================
    private User getUserFromAuthHeader(String authHeader){
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            throw new RuntimeException("Invalid Authorization header");
        }

        String token = authHeader.substring(7);
        String phoneNumber = jwtUtil.extractPhoneNumber(token);

        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
