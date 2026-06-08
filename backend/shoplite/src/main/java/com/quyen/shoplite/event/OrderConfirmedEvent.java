package com.quyen.shoplite.event;

import com.quyen.shoplite.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderConfirmedEvent {
    private final Order order;
}
