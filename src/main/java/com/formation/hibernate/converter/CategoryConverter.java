package com.formation.hibernate.converter;

import com.formation.hibernate.dto.CategoryDto;
import com.formation.hibernate.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryConverter {

    public CategoryDto toDto(Category category) {
        if (category == null) return null;

        return new CategoryDto(
            category.getId(),
            category.getName(),
            category.getDescription()
        );
    }

    public List<CategoryDto> toDtoList(List<Category> categories) {
        if (categories == null) return null;
        return categories.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public Category toEntity(CategoryDto dto) {
        if (dto == null) return null;

        Category category = new Category();
        category.setId(dto.getId());
        category.setName(dto.getName());
        category.setDescription(dto.getDescription());

        return category;
    }
}