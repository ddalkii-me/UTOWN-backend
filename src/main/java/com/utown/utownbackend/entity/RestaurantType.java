package com.utown.utownbackend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantType extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private String description;

}