package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionGroupRequestDto;
import com.utown.utownbackend.dto.DishOptionGroupResponseDto;
import com.utown.utownbackend.entity.Dish;
import com.utown.utownbackend.entity.DishOptionGroup;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishOptionGroupServiceImpl implements DishOptionGroupService {

    private final DishOptionGroupRepository dishOptionGroupRepository;
    private final DishRepository dishRepository;

    @Override
    @Transactional
    public DishOptionGroupResponseDto createDishOptionGroup(DishOptionGroupRequestDto request) {
        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(request.dishId())
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));

        DishOptionGroup group = new DishOptionGroup();
        group.setDish(dish);
        group.setName(request.name());
        group.setRequired(request.required());
        group.setMinSelections(request.minSelections());
        group.setMaxSelections(request.maxSelections());
        group.setSortOrder(request.sortOrder());

        DishOptionGroup savedGroup = dishOptionGroupRepository.save(group);
        return toDto(savedGroup);
    }

    @Override
    public List<DishOptionGroupResponseDto> getAllDishOptionGroups() {
        return dishOptionGroupRepository.findAllByDeletedAtIsNull().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishOptionGroupResponseDto> getDishOptionGroupsByDishId(Long dishId) {
        dishRepository.findByIdAndDeletedAtIsNull(dishId)
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));

        return dishOptionGroupRepository.findAllByDishIdAndDeletedAtIsNull(dishId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DishOptionGroupResponseDto getDishOptionGroupById(Long id) {
        return dishOptionGroupRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));
    }

    @Override
    @Transactional
    public DishOptionGroupResponseDto updateDishOptionGroup(Long id, DishOptionGroupRequestDto request) {
        DishOptionGroup group = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        Dish dish = dishRepository.findByIdAndDeletedAtIsNull(request.dishId())
                .orElseThrow(() -> new EntityNotFoundException("Dish not found"));

        group.setDish(dish);
        group.setName(request.name());
        group.setRequired(request.required());
        group.setMinSelections(request.minSelections());
        group.setMaxSelections(request.maxSelections());
        group.setSortOrder(request.sortOrder());

        DishOptionGroup updatedGroup = dishOptionGroupRepository.save(group);
        return toDto(updatedGroup);
    }

    @Override
    @Transactional
    public void deleteDishOptionGroup(Long id) {
        DishOptionGroup group = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));
        group.setDeletedAt(LocalDateTime.now());
        dishOptionGroupRepository.save(group);
    }

    private DishOptionGroupResponseDto toDto(DishOptionGroup group) {
        return new DishOptionGroupResponseDto(
                group.getId(),
                group.getDish().getId(),
                group.getName(),
                group.getRequired(),
                group.getMinSelections(),
                group.getMaxSelections(),
                group.getSortOrder(),
                group.getDeletedAt()
        );
    }
}
