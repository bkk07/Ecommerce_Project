package com.ecommerce.userservice.api.controller;

import com.ecommerce.userservice.api.dto.AddressRequest;
import com.ecommerce.userservice.api.dto.UserResponse;
import com.ecommerce.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile and address management endpoints")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user profile", description = "Retrieves user profile by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID") @PathVariable Long id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Add address", description = "Adds a new address to user's profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Address added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid address data"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/{id}/address")
    public ResponseEntity<?> addAddress(
            @Parameter(description = "User ID") @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {
        userService.addAddress(id, request);
        return ResponseEntity.ok(Map.of("message", "Address added successfully"));
    }
}