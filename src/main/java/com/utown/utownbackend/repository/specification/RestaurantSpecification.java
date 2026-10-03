package com.utown.utownbackend.repository.specification;

import com.utown.utownbackend.entity.City;
import com.utown.utownbackend.entity.Restaurant;
import com.utown.utownbackend.entity.RestaurantType;
import com.utown.utownbackend.entity.RestaurantStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class RestaurantSpecification {

    public static Specification<Restaurant> getSearchSpecification(
            Long cityId,
            String typeName,
            BigDecimal minRating,
            String searchKeyword,
            RestaurantStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted rows
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (cityId != null) {
                Join<Restaurant, City> cityJoin = root.join("city");
                predicates.add(cb.equal(cityJoin.get("id"), cityId));
            }

            if (StringUtils.hasText(typeName)) {
                Join<Restaurant, RestaurantType> typeJoin = root.join("type");
                predicates.add(cb.equal(cb.upper(typeJoin.get("name")), typeName.toUpperCase()));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("averageRating"), minRating));
            }

            if (StringUtils.hasText(searchKeyword)) {
                String pattern = "%" + searchKeyword.toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
