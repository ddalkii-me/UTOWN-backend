package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @EntityGraph(attributePaths = {"dish"})
    List<CartItem> findAllByCartId(Long cartId);

    @EntityGraph(attributePaths = {"dish"})
    Optional<CartItem> findByIdAndCartId(Long id, Long cartId);

    void deleteAllByCartId(Long cartId);
}
