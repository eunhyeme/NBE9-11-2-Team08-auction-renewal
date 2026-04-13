package com.team8.backend.domain.membership.controller;

import com.team8.backend.domain.membership.service.MembershipApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membership-applications")
@RequiredArgsConstructor
public class MembershipApplicationController {

    private final MembershipApplicationService membershipApplicationService;
}
