package com.team8.backend.domain.membership.service;

import com.team8.backend.domain.membership.repository.MembershipApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
}
