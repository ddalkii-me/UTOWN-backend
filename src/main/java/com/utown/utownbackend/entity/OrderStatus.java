package com.utown.utownbackend.entity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle states.
 *
 * <p>The allowed transitions are declared once in {@link #TRANSITIONS} and are the single
 * source of truth for the order lifecycle:
 *
 * <pre>
 * PENDING ──► ACCEPTED ──► IN_PREPARATION ──► READY_FOR_PICKUP ──► OUT_FOR_DELIVERY ──► DELIVERED ──► COMPLETED
 *    │
 *    ├──► DECLINED   (restaurant rejects)
 *    └──► CANCELLED  (customer cancels)
 * </pre>
 *
 * {@code COMPLETED}, {@code DECLINED} and {@code CANCELLED} are terminal.
 */
public enum OrderStatus {

    PENDING,
    ACCEPTED,
    IN_PREPARATION,
    READY_FOR_PICKUP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    COMPLETED,
    DECLINED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(PENDING, EnumSet.of(ACCEPTED, DECLINED, CANCELLED));
        TRANSITIONS.put(ACCEPTED, EnumSet.of(IN_PREPARATION));
        TRANSITIONS.put(IN_PREPARATION, EnumSet.of(READY_FOR_PICKUP));
        TRANSITIONS.put(READY_FOR_PICKUP, EnumSet.of(OUT_FOR_DELIVERY));
        TRANSITIONS.put(OUT_FOR_DELIVERY, EnumSet.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, EnumSet.of(COMPLETED));
        TRANSITIONS.put(COMPLETED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(DECLINED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    /** Statuses this status may move to directly. */
    public Set<OrderStatus> allowedTransitions() {
        return Collections.unmodifiableSet(TRANSITIONS.get(this));
    }

    public boolean canTransitionTo(OrderStatus target) {
        return target != null && TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return TRANSITIONS.get(this).isEmpty();
    }
}
