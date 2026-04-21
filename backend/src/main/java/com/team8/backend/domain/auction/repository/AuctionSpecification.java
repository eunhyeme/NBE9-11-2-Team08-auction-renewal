package com.team8.backend.domain.auction.repository;

import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class AuctionSpecification {

    public static Specification<Auction> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<Auction> statusIn(List<AuctionStatus> statuses) {
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Auction> titleContains(String keyword) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Auction> minPrice(Long min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("currentPrice"), min);
    }

    public static Specification<Auction> maxPrice(Long max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("currentPrice"), max);
    }

}
