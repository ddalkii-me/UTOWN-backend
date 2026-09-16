package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Cart;
import com.utown.utownbackend.entity.CartStatus;
import com.utown.utownbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);

    Optional<Cart> findByUser(User user);

    void deleteByUserId(Long userId);
}
