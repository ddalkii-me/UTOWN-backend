package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.AuthCode;
import com.utown.utownbackend.entity.AuthCodePurpose;
import com.utown.utownbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    Optional<AuthCode> findTopByUserAndPurposeOrderByCreatedAtDesc(User user, AuthCodePurpose purpose);

    Optional<AuthCode> findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(User user, AuthCodePurpose purpose);
}
