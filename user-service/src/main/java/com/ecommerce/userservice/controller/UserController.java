package com.ecommerce.userservice.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader("X-Auth-User-Id") String userId) {
        // The Gateway verified the token and extracted the ID for us.
        // We just use the ID to fetch data.
        return ResponseEntity.ok(Map.of(
                "id", userId,
                "name", "John Doe",
                "email", "john@example.com",
                "status", "Verified via Gateway"
        ));
    }
}