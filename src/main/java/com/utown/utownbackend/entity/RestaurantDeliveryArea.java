package com.utown.utownbackend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "restaurant_delivery_areas",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"restaurant_id", "delivery_area_id"}
                )
        }
)
public class RestaurantDeliveryArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "delivery_area_id", nullable = false)
    private DeliveryArea deliveryArea;

    public RestaurantDeliveryArea() {
    }

    public RestaurantDeliveryArea(
            Restaurant restaurant,
            DeliveryArea deliveryArea
    ) {
        this.restaurant = restaurant;
        this.deliveryArea = deliveryArea;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public DeliveryArea getDeliveryArea() {
        return deliveryArea;
    }

    public void setDeliveryArea(DeliveryArea deliveryArea) {
        this.deliveryArea = deliveryArea;
    }
}