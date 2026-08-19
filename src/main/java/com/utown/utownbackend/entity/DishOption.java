package com.utown.utownbackend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dish_options")
public class DishOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "option_group_id", nullable = false)
    private DishOptionGroup optionGroup;

    @Column(nullable = false)
    private String name;

    @Column(name = "additional_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal additionalPrice;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private String status;

    public DishOption() {
    }

    public DishOption(
            DishOptionGroup optionGroup,
            String name,
            BigDecimal additionalPrice,
            Integer sortOrder,
            String status
    ) {
        this.optionGroup = optionGroup;
        this.name = name;
        this.additionalPrice = additionalPrice;
        this.sortOrder = sortOrder;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DishOptionGroup getOptionGroup() {
        return optionGroup;
    }

    public void setOptionGroup(DishOptionGroup optionGroup) {
        this.optionGroup = optionGroup;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getAdditionalPrice() {
        return additionalPrice;
    }

    public void setAdditionalPrice(BigDecimal additionalPrice) {
        this.additionalPrice = additionalPrice;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}