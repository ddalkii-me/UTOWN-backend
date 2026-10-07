package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;
import com.utown.utownbackend.entity.DishOption;
import com.utown.utownbackend.entity.DishOptionGroup;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DishOptionServiceImpl implements DishOptionService {

    private final DishOptionRepository dishOptionRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;
    private final CacheManager cacheManager;

    @Override
    @Transactional
    public DishOptionResponseDto createDishOption(DishOptionRequestDto request) {
        log.info("Executing createDishOption");
        DishOptionGroup group = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(request.optionGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        DishOption option = new DishOption();
        option.setOptionGroup(group);
        option.setName(request.name());
        option.setAdditionalPrice(request.additionalPrice());
        option.setSortOrder(request.sortOrder());
        option.setStatus(request.status());

        DishOption savedOption = dishOptionRepository.save(option);

        evictRestaurantMenu(group.getDish().getRestaurant().getId());

        return toDto(savedOption);
    }

    @Override
    public List<DishOptionResponseDto> getAllDishOptions() {
        log.info("Executing getAllDishOptions");
        return dishOptionRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishOptionResponseDto> getDishOptionsByGroupId(Long optionGroupId) {
        log.info("Executing getDishOptionsByGroupId with optionGroupId={}", optionGroupId);
        dishOptionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        return dishOptionRepository.findAllByOptionGroupIdAndDeletedAtIsNullOrderBySortOrderAsc(optionGroupId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DishOptionResponseDto getDishOptionById(Long id) {
        log.info("Executing getDishOptionById with id={}", id);
        return dishOptionRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option not found"));
    }

    @Override
    @Transactional
    public DishOptionResponseDto updateDishOption(Long id, DishOptionRequestDto request) {
        log.info("Executing updateDishOption with id={}", id);
        DishOption option = dishOptionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option not found"));

        Long previousRestaurantId = option.getOptionGroup().getDish().getRestaurant().getId();

        DishOptionGroup group = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(request.optionGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        option.setOptionGroup(group);
        option.setName(request.name());
        option.setAdditionalPrice(request.additionalPrice());
        option.setSortOrder(request.sortOrder());
        option.setStatus(request.status());

        DishOption updatedOption = dishOptionRepository.save(option);

        evictRestaurantMenu(previousRestaurantId);
        evictRestaurantMenu(group.getDish().getRestaurant().getId());

        return toDto(updatedOption);
    }

    @Override
    @Transactional
    public void deleteDishOption(Long id) {
        log.info("Executing deleteDishOption with id={}", id);
        DishOption option = dishOptionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option not found"));
        option.setDeletedAt(LocalDateTime.now());

        Long restaurantId = option.getOptionGroup().getDish().getRestaurant().getId();

        dishOptionRepository.save(option);

        evictRestaurantMenu(restaurantId);
    }

    private void evictRestaurantMenu(Long restaurantId) {
        var menuCache = cacheManager.getCache("restaurantMenus");
        if (menuCache != null) {
            menuCache.evict(restaurantId);
        }
    }

    private DishOptionResponseDto toDto(DishOption option) {
        return new DishOptionResponseDto(
                option.getId(),
                option.getOptionGroup().getId(),
                option.getName(),
                option.getAdditionalPrice(),
                option.getSortOrder(),
                option.getStatus(),
                option.getDeletedAt()
        );
    }
}
