package com.ecommerce.userservice.infrastructure.repository;

import com.ecommerce.userservice.domain.model.User;
import com.ecommerce.userservice.domain.port.UserRepositoryPort;
import com.ecommerce.userservice.infrastructure.entity.UserEntity;
import com.ecommerce.userservice.infrastructure.mapper.UserPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // Import this

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final JpaUserRepo jpaUserRepo;
    private final UserPersistenceMapper mapper;

    @Override
    @Transactional // Write transaction for saves
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        return mapper.toDomain(jpaUserRepo.save(entity));
    }

    @Override
    @Transactional(readOnly = true) // Keep session open for reading + mapping
    public Optional<User> findByEmail(String email) {
        // The session stays open here, so the mapper can lazy-load addresses
        return jpaUserRepo.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return jpaUserRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return jpaUserRepo.findByEmail(email).isPresent();
    }
}