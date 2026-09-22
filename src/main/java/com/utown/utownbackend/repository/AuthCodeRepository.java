package com.utown.utownbackend.repository;

import com.utown.utownbackend.entity.AuthCode;
import com.utown.utownbackend.entity.AuthCodePurpose;
import com.utown.utownbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AuthCodeRepository extends JpaRepository<AuthCode, Long> {

    Optional<AuthCode> findTopByUserAndPurposeOrderByCreatedAtDesc(User user, AuthCodePurpose purpose);

    Optional<AuthCode> findTopByUserAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(User user, AuthCodePurpose purpose);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE AuthCode a SET a.attempts = a.attempts + 1 WHERE a.id = :id AND a.attempts < :maxAttempts")
    int incrementAttemptsIfUnderLimit(@Param("id") Long id, @Param("maxAttempts") int maxAttempts);

    @Modifying
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE AuthCode a SET a.attempts = a.attempts + 1 WHERE a.id = :id")
    void incrementAttempts(@Param("id") Long id);
}
