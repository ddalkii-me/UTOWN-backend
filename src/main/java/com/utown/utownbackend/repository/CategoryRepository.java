package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}