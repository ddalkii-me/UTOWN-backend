package com.utown.utownbackend.service;

import com.utown.utownbackend.dto.DishOptionRequestDto;
import com.utown.utownbackend.dto.DishOptionResponseDto;
import com.utown.utownbackend.entity.DishOption;
import com.utown.utownbackend.entity.DishOptionGroup;
import com.utown.utownbackend.repository.DishOptionGroupRepository;
import com.utown.utownbackend.repository.DishOptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishOptionServiceImpl implements DishOptionService {

    private final DishOptionRepository dishOptionRepository;
    private final DishOptionGroupRepository dishOptionGroupRepository;

    @Override
    @Transactional
    public DishOptionResponseDto createDishOption(DishOptionRequestDto request) {
        DishOptionGroup group = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(request.optionGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        DishOption option = new DishOption();
        option.setOptionGroup(group);
        option.setName(request.name());
        option.setAdditionalPrice(request.additionalPrice());
        option.setSortOrder(request.sortOrder());
        option.setStatus(request.status());

        DishOption savedOption = dishOptionRepository.save(option);
        return toDto(savedOption);
    }

    @Override
    public List<DishOptionResponseDto> getAllDishOptions() {
        return dishOptionRepository.findAllByDeletedAtIsNull().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<DishOptionResponseDto> getDishOptionsByGroupId(Long optionGroupId) {
        dishOptionGroupRepository.findByIdAndDeletedAtIsNull(optionGroupId)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        return dishOptionRepository.findAllByOptionGroupIdAndDeletedAtIsNull(optionGroupId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public DishOptionResponseDto getDishOptionById(Long id) {
        return dishOptionRepository.findByIdAndDeletedAtIsNull(id)
                .map(this::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option not found"));
    }

    @Override
    @Transactional
    public DishOptionResponseDto updateDishOption(Long id, DishOptionRequestDto request) {
        DishOption option = dishOptionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option not found"));

        DishOptionGroup group = dishOptionGroupRepository.findByIdAndDeletedAtIsNull(request.optionGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Dish Option Group not found"));

        option.setOptionGroup(group);
        option.setName(request.name());
        option.setAdditionalPrice(request.additionalPrice());
        option.setSortOrder(request.sortOrder());
        option.setStatus(request.status());

        DishOption updatedOption = dishOptionRepository.save(option);
        return toDto(updatedOption);
    }

    @Override
    @Transactional
    public void deleteDishOption(Long id) {
        DishOption option = dishOptionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish Option not found"));
        option.setDeletedAt(LocalDateTime.now());
        dishOptionRepository.save(option);
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
