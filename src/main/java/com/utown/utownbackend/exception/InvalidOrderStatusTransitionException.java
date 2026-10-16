package com.utown.utownbackend.exception;

import com.utown.utownbackend.entity.OrderStatus;
import lombok.Getter;

/**
 * Thrown when an order is asked to move to a status that the lifecycle does not allow
 * from its current status. Mapped to HTTP 409 Conflict.
 *
 * <p>Extends {@link IllegalStateException} so existing callers that catch the generic
 * type keep working.
 */
@Getter
public class InvalidOrderStatusTransitionException extends IllegalStateException {

    private final OrderStatus currentStatus;
    private final OrderStatus targetStatus;

    public InvalidOrderStatusTransitionException(OrderStatus currentStatus, OrderStatus targetStatus) {
        super("Order cannot transition from " + currentStatus + " to " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }
}
