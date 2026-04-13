package com.team8.backend.domain.membership.entity;

import com.team8.backend.domain.user.entity.User;
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

    public MembershipApplication(User user, MembershipStatus status, String reason, LocalDateTime requestedAt) {
        this.user = user;
        this.status = status;
        this.reason = reason;
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
        this.status = MembershipStatus.APPROVED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }

    public void reject(Long reviewerId, LocalDateTime reviewedAt) {
        this.status = MembershipStatus.REJECTED;
        this.reviewedBy = reviewerId;
        this.reviewedAt = reviewedAt;
    }
}
