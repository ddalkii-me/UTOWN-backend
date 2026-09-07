package com.utown.utownbackend.dto;

import com.utown.utownbackend.entity.DishOptionStatus;
import com.utown.utownbackend.entity.DishStatus;
import com.utown.utownbackend.entity.RestaurantStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("AddressRequestDto - valid dto should have no violations")
    void addressRequestDto_valid() {
        AddressRequestDto dto = new AddressRequestDto(
                1L, 1L, 1L, "Home", "Jane Doe", "010-1234-5678",
                "123 Main St", "12345", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0)
        );
        Set<ConstraintViolation<AddressRequestDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AddressRequestDto - missing required fields should trigger violations")
    void addressRequestDto_invalid() {
        AddressRequestDto dto = new AddressRequestDto(
                null, null, null, "Home", "", "010-1234-5678",
                "", "12345", BigDecimal.valueOf(37.5), BigDecimal.valueOf(127.0)
        );
        Set<ConstraintViolation<AddressRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(5);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("userId", "cityId", "deliveryAreaId", "recipientName", "addressLine");
    }

    @Test
    @DisplayName("CityRequestDto - blank name should trigger violation")
    void cityRequestDto_invalid() {
        CityRequestDto dto = new CityRequestDto("   ");
        Set<ConstraintViolation<CityRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    @DisplayName("CategoryRequestDto - missing restaurantId and blank name should trigger violations")
    void categoryRequestDto_invalid() {
        CategoryRequestDto dto = new CategoryRequestDto(null, "", "desc", "url", 1);
        Set<ConstraintViolation<CategoryRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("restaurantId", "name");
    }

    @Test
    @DisplayName("DeliveryAreaRequestDto - null cityId and blank name should trigger violations")
    void deliveryAreaRequestDto_invalid() {
        DeliveryAreaRequestDto dto = new DeliveryAreaRequestDto(null, "");
        Set<ConstraintViolation<DeliveryAreaRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(2);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("cityId", "name");
    }

    @Test
    @DisplayName("DishRequestDto - missing required fields should trigger violations")
    void dishRequestDto_invalid() {
        DishRequestDto dto = new DishRequestDto(null, null, "  ", null, "desc", "url", null, 1);
        Set<ConstraintViolation<DishRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(5);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("restaurantId", "categoryId", "name", "price", "status");
    }

    @Test
    @DisplayName("DishOptionGroupRequestDto - missing required fields and negative selections trigger violations")
    void dishOptionGroupRequestDto_invalid() {
        DishOptionGroupRequestDto dto = new DishOptionGroupRequestDto(null, "", null, -1, -2, 1);
        Set<ConstraintViolation<DishOptionGroupRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(5);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("dishId", "name", "required", "minSelections", "maxSelections");
    }

    @Test
    @DisplayName("DishOptionRequestDto - missing required fields should trigger violations")
    void dishOptionRequestDto_invalid() {
        DishOptionRequestDto dto = new DishOptionRequestDto("", null, null, 1, null);
        Set<ConstraintViolation<DishOptionRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(4);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("name", "optionGroupId", "additionalPrice", "status");
    }

    @Test
    @DisplayName("RestaurantRequestDto - missing required fields should trigger violations")
    void restaurantRequestDto_invalid() {
        RestaurantRequestDto dto = new RestaurantRequestDto(
                null, null, null, "  ", "desc", "addr", "phone", "logo",
                null, null, null, null
        );
        Set<ConstraintViolation<RestaurantRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(5);
        assertThat(violations).extracting(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .containsExactlyInAnyOrder("ownerId", "typeId", "cityId", "name", "status");
    }

    @Test
    @DisplayName("RestaurantTypeRequestDto - blank name should trigger violation")
    void restaurantTypeRequestDto_invalid() {
        RestaurantTypeRequestDto dto = new RestaurantTypeRequestDto("", "desc");
        Set<ConstraintViolation<RestaurantTypeRequestDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("name");
    }

    @Test
    @DisplayName("WorkingHoursDto - null dayOfWeek should trigger violation")
    void workingHoursDto_invalid() {
        WorkingHoursDto dto = new WorkingHoursDto(null, LocalTime.of(9, 0), LocalTime.of(22, 0), false);
        Set<ConstraintViolation<WorkingHoursDto>> violations = validator.validate(dto);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("dayOfWeek");
    }
}
