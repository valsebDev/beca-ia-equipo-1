package com.ids.ms_catalog.mapper;

import com.ids.ms_catalog.dto.request.CategoryRequest;
import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.model.Category;

public final class CategoryMapper {

	private CategoryMapper() {
	}

	public static Category toEntity(CategoryRequest request) {
		Category category = new Category();
		category.setActive(true);
		updateEntity(category, request);
		return category;
	}

	public static void updateEntity(Category category, CategoryRequest request) {
		category.setName(request.name());
		category.setDescription(request.description());
	}

	public static CategoryResponse toResponse(Category category) {
		return new CategoryResponse(category.getId(), category.getName(), category.getDescription(),
				category.isActive());
	}
}
