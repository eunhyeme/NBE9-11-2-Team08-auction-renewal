package com.team8.backend.domain.membership.entity;

import com.team8.backend.domain.user.entity.User;
import com.team8.backend.global.exception.CustomException;
import com.team8.backend.global.exception.ErrorCode;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_application")
public class MembershipApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipStatus status;

    private String reason;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    protected MembershipApplication() {}

    public MembershipApplication(User user, LocalDateTime requestedAt) {
        this.user = user;
        this.status = MembershipStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public MembershipStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public Long getReviewedBy() { return reviewedBy; }

    public void approve(Long reviewerId, LocalDateTime reviewedAt) {
        validatePending();
        this.status = MembershipStatus.APPROVED;
        this.reason = null;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    public void reject(Long reviewerId, LocalDateTime reviewedAt, String reason) {
        validatePending();
        if (reason == null || reason.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.status = MembershipStatus.REJECTED;
        this.reason = reason;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    private void validatePending() {
        if (this.status != MembershipStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_APPLICATION_STATE);
        }
    }
}
