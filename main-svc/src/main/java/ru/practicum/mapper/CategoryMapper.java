package ru.practicum.mapper;

import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.model.Category;

public class CategoryMapper {

    public static CategoryDto toCategoryDto(Category category) {
        if (category == null) {
            return null;
        }
        CategoryDto dto = new CategoryDto();
        dto.setId(category.getId());
        dto.setName(category.getName());
        return dto;
    }

    public static Category toCategory(CategoryDto categoryDto) {
        if (categoryDto == null) {
            return null;
        }
        Category category = new Category();
        category.setId(categoryDto.getId());
        category.setName(categoryDto.getName());
        return category;
    }

    public static Category toCategory(NewCategoryDto newCategoryDto) {
        if (newCategoryDto == null) {
            return null;
        }
        Category category = new Category();
        category.setName(newCategoryDto.getName());
        return category;
    }
}