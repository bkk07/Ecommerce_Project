package com.ecommerce.userservice.service;

import com.ecommerce.userservice.api.dto.AddressRequest;
import com.ecommerce.userservice.api.dto.UserResponse;
import com.ecommerce.userservice.domain.model.Address;
import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.domain.port.UserRepositoryPort;
import com.ecommerce.userservice.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepositoryPort userRepository;

    private User getUserByIdPrivate(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }
    public UserResponse getUserById(Long id) {
        return mapToUserResponse(getUserByIdPrivate(id));
    }
    public void addAddress(Long userId, AddressRequest request) {
        User user = getUserByIdPrivate(userId);
        // Convert DTO to Domain
        Address newAddress = Address.builder()
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .zipCode(request.getZipCode())
                .country(request.getCountry())
                .type(request.getType())
                .build();

        // Initialize list if null (safety check)
        if (user.getAddresses() == null) {
            user.setAddresses(new ArrayList<>());
        }
        // Add to Domain Object
        user.getAddresses().add(newAddress);
        // Save entire User (Cascade will handle Address table)
        userRepository.save(user);
    }
    private UserResponse  mapToUserResponse(User user) {
        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setEmail(user.getEmail());
        userResponse.setName(user.getName());
        userResponse.setPhone(user.getPhone());
        userResponse.setAddresses(user.getAddresses());
        return userResponse;
    }
}