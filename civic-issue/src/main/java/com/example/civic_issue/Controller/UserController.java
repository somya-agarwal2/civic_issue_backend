package com.example.civic_issue.Controller;

import com.example.civic_issue.dto.UserProfileDto;
import com.example.civic_issue.Model.User;
import com.example.civic_issue.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")  // ✅ use plural consistently
@CrossOrigin(origins = "*")
public class UserController {

    private final String UPLOAD_DIR = "uploads/";

    @Autowired
    private UserRepository userRepository;


    // ✅ Add new user
    @PostMapping("/add")
    public User addUser(@RequestBody User user) {
        return userRepository.save(user);
    }

    // ✅ Get all users
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ✅ Get user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(user);
    }

    // ✅ Upload / Skip Profile Picture
    @PostMapping("/{id}/uploadPhoto")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable Long id,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "skip", required = false, defaultValue = "false") boolean skip
    ) throws IOException {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (skip) {
            return ResponseEntity.ok(user); // keep old photo
        }

        if (file != null && !file.isEmpty()) {
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            File dest = new File(UPLOAD_DIR + fileName);
            file.transferTo(dest);

            user.setPhotoUrl("/uploads/" + fileName);
            userRepository.save(user);

            return ResponseEntity.ok(user);
        }

        return ResponseEntity.badRequest().body("No file uploaded and skip not set");
    }
}
