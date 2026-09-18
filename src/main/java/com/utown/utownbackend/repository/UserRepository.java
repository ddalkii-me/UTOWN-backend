package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.User;
import com.utown.utownbackend.entity.UserRole;
import com.utown.utownbackend.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    Optional<User> findByPhoneAndDeletedAtIsNull(String phone);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);

    List<User> findAllByDeletedAtIsNull();

    List<User> findByRoleAndDeletedAtIsNull(UserRole role);

    List<User> findByStatusAndDeletedAtIsNull(UserStatus status);

    List<User> findByRoleAndStatusAndDeletedAtIsNull(UserRole role, UserStatus status);
}