package com.utown.utownbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dish_option_groups")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DishOptionGroup extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dish_id", nullable = false)
    private Dish dish;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Boolean required;

    @Column(name = "min_selections")
    private Integer minSelections;

    @Column(name = "max_selections")
    private Integer maxSelections;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}