package com.ecommerce.checkoutservice.repository;


import com.ecommerce.checkoutservice.entity.CheckoutSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckoutSessionRepository extends CrudRepository<CheckoutSession, String> {
}