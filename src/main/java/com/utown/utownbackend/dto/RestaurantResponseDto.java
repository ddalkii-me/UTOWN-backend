package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.RestaurantStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantResponseDto {
    private Long id;
    private Long ownerId;
    private Long typeId;
    private Long cityId;

    private String name;
    private String description;
    private String address;
    private String phone;
    private String logoUrl;

    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal minimumOrderAmount;

    private RestaurantStatus status;
}
