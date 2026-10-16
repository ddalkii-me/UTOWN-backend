package com.utown.utownbackend.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @ParameterizedTest(name = "Transition from {0} to {1} should be valid={2}")
    @MethodSource("provideAllTransitions")
    @DisplayName("canTransitionTo - exhaustively tests all possible transition combinations")
    void canTransitionTo_exhaustive(OrderStatus currentStatus, OrderStatus targetStatus, boolean expectedIsValid) {
        assertThat(currentStatus.canTransitionTo(targetStatus)).isEqualTo(expectedIsValid);
    }

    private static Stream<Arguments> provideAllTransitions() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (OrderStatus current : OrderStatus.values()) {
            for (OrderStatus target : OrderStatus.values()) {
                boolean expected = getValidTransitionsFor(current).contains(target);
                builder.add(Arguments.of(current, target, expected));
            }
        }
        return builder.build();
    }

    private static Set<OrderStatus> getValidTransitionsFor(OrderStatus current) {
        return switch (current) {
            case PENDING -> EnumSet.of(OrderStatus.ACCEPTED, OrderStatus.DECLINED, OrderStatus.CANCELLED);
            case ACCEPTED -> EnumSet.of(OrderStatus.IN_PREPARATION);
            case IN_PREPARATION -> EnumSet.of(OrderStatus.READY_FOR_PICKUP);
            case READY_FOR_PICKUP -> EnumSet.of(OrderStatus.OUT_FOR_DELIVERY);
            case OUT_FOR_DELIVERY -> EnumSet.of(OrderStatus.DELIVERED);
            case DELIVERED -> EnumSet.of(OrderStatus.COMPLETED);
            case COMPLETED -> EnumSet.noneOf(OrderStatus.class);
            case DECLINED -> EnumSet.noneOf(OrderStatus.class);
            case CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }
}
