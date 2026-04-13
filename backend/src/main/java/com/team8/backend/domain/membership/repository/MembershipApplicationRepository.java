package com.team8.backend.domain.membership.repository;

import com.team8.backend.domain.membership.entity.MembershipApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipApplicationRepository extends JpaRepository<MembershipApplication, Long> {
}
