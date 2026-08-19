package com.utown.utownbackend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "cart_item_options")
public class CartItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_item_id", nullable = false)
    private CartItem cartItem;

    @ManyToOne
    @JoinColumn(name = "dish_option_id", nullable = false)
    private DishOption dishOption;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "option_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal optionPrice;

    public CartItemOption() {
    }

    public CartItemOption(
            CartItem cartItem,
            DishOption dishOption,
            String optionName,
            BigDecimal optionPrice
    ) {
        this.cartItem = cartItem;
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

    public CartItem getCartItem() {
        return cartItem;
    }

    public void setCartItem(CartItem cartItem) {
        this.cartItem = cartItem;
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