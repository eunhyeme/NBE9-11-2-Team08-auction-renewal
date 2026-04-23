package com.team8.backend.global.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuctionEvent<T> {
    private String type; // "BID", "END"
    private T data;
}