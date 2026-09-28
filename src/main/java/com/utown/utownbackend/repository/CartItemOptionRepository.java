package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.CartItemOption;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemOptionRepository extends JpaRepository<CartItemOption, Long> {

    @EntityGraph(attributePaths = {"dishOption"})
    List<CartItemOption> findAllByCartItemId(Long cartItemId);

    @EntityGraph(attributePaths = {"dishOption"})
    List<CartItemOption> findAllByCartItemIdIn(List<Long> cartItemIds);

    void deleteAllByCartItemId(Long cartItemId);

    void deleteAllByCartItemIdIn(List<Long> cartItemIds);
}
