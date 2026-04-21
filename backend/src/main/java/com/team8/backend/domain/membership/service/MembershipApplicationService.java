package com.team8.backend.domain.membership.service;

import com.team8.backend.domain.membership.dto.request.MembershipReviewRequest;
import com.team8.backend.domain.membership.dto.response.MembershipApplyResponse;
import com.team8.backend.domain.membership.dto.response.MembershipApplicationSummaryResponse;
import com.team8.backend.domain.membership.entity.MembershipApplication;
import com.team8.backend.domain.membership.entity.MembershipStatus;
import com.team8.backend.domain.membership.repository.MembershipApplicationRepository;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.entity.UserPermission;
import com.team8.backend.domain.user.repository.UserRepository;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import com.team8.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MembershipApplicationService {

    private final MembershipApplicationRepository membershipApplicationRepository;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('ADMIN')")
    public List<MembershipApplicationSummaryResponse> getApplications(CustomUserDetails userDetails) {
        return membershipApplicationRepository.findAllByOrderByRequestedAtDesc().stream()
                .map(MembershipApplicationSummaryResponse::from)
                .toList();
    }

    @PreAuthorize("hasRole('USER')")
    @Transactional
    public MembershipApplyResponse apply(CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean hasPendingApplication = membershipApplicationRepository
                .existsByUser_UserIdAndStatus(userId, MembershipStatus.PENDING);

        if (hasPendingApplication) {
            throw new CustomException(ErrorCode.ALREADY_APPLIED);
        }
        MembershipApplication application = new MembershipApplication(user, LocalDateTime.now());
        MembershipApplication savedApplication = membershipApplicationRepository.save(application);

        return MembershipApplyResponse.of(savedApplication.getId());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public MembershipApplicationSummaryResponse review(
            Long applicationId,
            MembershipReviewRequest request,
            CustomUserDetails userDetails
    ) {
        MembershipApplication application = membershipApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND));

        Long reviewerId = userDetails.getUserId();
        LocalDateTime reviewedAt = LocalDateTime.now();

        if (request.status() == MembershipStatus.APPROVED) {
            application.approve(reviewerId, reviewedAt);
            application.getUser().grantPermission(UserPermission.SELLER);
        } else if (request.status() == MembershipStatus.REJECTED) {
            application.reject(reviewerId, reviewedAt, request.reason());
        } else {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return MembershipApplicationSummaryResponse.from(application);
    }
}
