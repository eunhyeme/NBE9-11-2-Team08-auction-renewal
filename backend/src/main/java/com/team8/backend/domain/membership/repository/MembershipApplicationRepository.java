package com.team8.backend.domain.membership.repository;

import com.team8.backend.domain.membership.entity.MembershipApplication;
import com.team8.backend.domain.membership.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MembershipApplicationRepository extends JpaRepository<MembershipApplication, Long> {
    boolean existsByUser_UserIdAndStatus(Long userId, MembershipStatus status);
    List<MembershipApplication> findAllByOrderByRequestedAtDesc();
}
