package com.utown.utownbackend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_options")
public class OrderItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne
    @JoinColumn(name = "dish_option_id", nullable = false)
    private DishOption dishOption;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "option_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal optionPrice;

    public OrderItemOption() {
    }

    public OrderItemOption(
            OrderItem orderItem,
            DishOption dishOption,
            String optionName,
            BigDecimal optionPrice
    ) {
        this.orderItem = orderItem;
        this.dishOption = dishOption;
        this.optionName = optionName;
        this.optionPrice = optionPrice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public DishOption getDishOption() {
        return dishOption;
    }

    public void setDishOption(DishOption dishOption) {
        this.dishOption = dishOption;
    }

    public String getOptionName() {
        return optionName;
    }

    public void setOptionName(String optionName) {
        this.optionName = optionName;
    }

    public BigDecimal getOptionPrice() {
        return optionPrice;
    }

    public void setOptionPrice(BigDecimal optionPrice) {
        this.optionPrice = optionPrice;
    }
}