package com.ecommerce.userservice.api.controller;

import com.ecommerce.userservice.api.dto.AddressRequest;
import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid; // Import this!
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Get User Profile
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    // Add Address
    @PostMapping("/{id}/address")
    public ResponseEntity<?> addAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request // @Valid triggers validation
    ) {
        userService.addAddress(id, request);
        return ResponseEntity.ok(Map.of("message", "Address added successfully"));
    }
}