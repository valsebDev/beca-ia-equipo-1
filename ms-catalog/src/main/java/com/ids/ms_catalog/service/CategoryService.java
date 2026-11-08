package com.ids.ms_catalog.service;

import com.ids.ms_catalog.dto.request.CategoryRequest;
import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.exception.ConflictException;
import com.ids.ms_catalog.exception.NotFoundException;
import com.ids.ms_catalog.mapper.CategoryMapper;
import com.ids.ms_catalog.model.Category;
import com.ids.ms_catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;

	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		ensureNameAvailable(request.name(), null);
		return CategoryMapper.toResponse(categoryRepository.save(CategoryMapper.toEntity(request)));
	}

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll(Boolean active) {
		List<Category> categories = active == null
				? categoryRepository.findAllByOrderByNameAsc()
				: categoryRepository.findAllByActiveOrderByNameAsc(active);
		return categories.stream().map(CategoryMapper::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CategoryResponse findById(Long id) {
		return CategoryMapper.toResponse(getOrThrow(id));
	}

	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = getOrThrow(id);
		ensureNameAvailable(request.name(), id);
		CategoryMapper.updateEntity(category, request);
		return CategoryMapper.toResponse(category);
	}

	@Transactional
	public void deactivate(Long id) {
		getOrThrow(id).setActive(false);
	}

	private Category getOrThrow(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "No existe una categoría con id " + id));
	}

	private void ensureNameAvailable(String name, Long currentId) {
		boolean taken = currentId == null
				? categoryRepository.existsByName(name)
				: categoryRepository.existsByNameAndIdNot(name, currentId);
		if (taken) {
			throw new ConflictException("CATEGORY_ALREADY_EXISTS", "Ya existe una categoría llamada \"" + name + "\"");
		}
	}
}
