package com.ecommerce.userservice.infrastructure.mapper;

import com.ecommerce.userservice.domain.model.Address;
import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.infrastructure.entity.AddressEntity;
import com.ecommerce.userservice.infrastructure.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .password(entity.getPassword())
                .phone(entity.getPhone())
                .role(entity.getRole())
                .isEmailVerified(entity.isEmailVerified())
                .isPhoneVerified(entity.isPhoneVerified())
                .emailVerificationOtp(entity.getEmailVerificationOtp())
                .emailOtpExpiry(entity.getEmailOtpExpiry())
                .phoneVerificationOtp(entity.getPhoneVerificationOtp())
                .phoneOtpExpiry(entity.getPhoneOtpExpiry())
                .forgotPasswordOtp(entity.getForgotPasswordOtp())
                .forgotPasswordOtpExpiry(entity.getForgotPasswordOtpExpiry())
                .isPasswordResetVerified(entity.isPasswordResetVerified())
                .addresses(mapEntitiesToAddresses(entity.getAddresses()))
                .build();
    }

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        UserEntity entity = new UserEntity();
        if(domain.getId() != null) entity.setId(domain.getId());

        entity.setName(domain.getName());
        entity.setEmail(domain.getEmail());
        entity.setPassword(domain.getPassword());
        entity.setPhone(domain.getPhone());
        entity.setRole(domain.getRole());
        entity.setEmailVerified(domain.isEmailVerified());
        entity.setPhoneVerified(domain.isPhoneVerified());

        // --- NEW MAPPINGS START ---
        entity.setEmailVerificationOtp(domain.getEmailVerificationOtp());
        entity.setEmailOtpExpiry(domain.getEmailOtpExpiry());
        entity.setPhoneVerificationOtp(domain.getPhoneVerificationOtp());
        entity.setPhoneOtpExpiry(domain.getPhoneOtpExpiry());
        entity.setForgotPasswordOtp(domain.getForgotPasswordOtp());
        entity.setForgotPasswordOtpExpiry(domain.getForgotPasswordOtpExpiry());
        entity.setPasswordResetVerified(domain.isPasswordResetVerified());
        // --- NEW MAPPINGS END ---

        // Map Addresses Domain -> Entity
        if (domain.getAddresses() != null) {
            List<AddressEntity> addressEntities = domain.getAddresses().stream().map(addr -> {
                AddressEntity ae = new AddressEntity();
                ae.setStreet(addr.getStreet());
                ae.setCity(addr.getCity());
                ae.setState(addr.getState());
                ae.setZipCode(addr.getZipCode());
                ae.setCountry(addr.getCountry());
                ae.setType(addr.getType());
                ae.setUser(entity);
                return ae;
            }).collect(Collectors.toList());
            entity.setAddresses(addressEntities);
        }
        return entity;
    }

    // Helper
    private List<Address> mapEntitiesToAddresses(List<AddressEntity> entities) {
        if (entities == null) return new ArrayList<>();
        return entities.stream().map(e -> Address.builder()
                .street(e.getStreet())
                .city(e.getCity())
                .state(e.getState())
                .zipCode(e.getZipCode())
                .country(e.getCountry())
                .type(e.getType())
                .build()
        ).collect(Collectors.toList());
    }
}