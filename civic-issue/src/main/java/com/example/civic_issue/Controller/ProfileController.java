package com.example.civic_issue.Controller;

import com.example.civic_issue.Model.User;
import com.example.civic_issue.dto.ProfileUpdateRequest;
import com.example.civic_issue.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @PutMapping("/update/{id}")
    public ResponseEntity<User> updateProfile(@PathVariable Long id,
                                              @RequestBody ProfileUpdateRequest request) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setFirstName(request.firstName());
                    user.setLastName(request.lastName());
                    user.setPhoneNumber(request.phoneNumber());
                    user.setAddress(request.address());
                    user.setProfileCompleted(true);
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
