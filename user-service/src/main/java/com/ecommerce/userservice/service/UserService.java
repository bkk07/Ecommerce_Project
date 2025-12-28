package com.ecommerce.userservice.service;

import com.ecommerce.userservice.api.dto.AddressRequest;
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

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));
    }

    public void addAddress(Long userId, AddressRequest request) {
        User user = getUserById(userId);

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
}