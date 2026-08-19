package com.utown.utownbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "delivery_area")
public class DeliveryArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private String name;

    public DeliveryArea() {
    }

    public DeliveryArea(City city, String name) {
        this.city = city;
        this.name = name;
    }

    // getters and setters
}