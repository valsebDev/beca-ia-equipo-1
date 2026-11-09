package com.ids.ms_catalog.controller;

import com.ids.ms_catalog.dto.request.CategoryRequest;
import com.ids.ms_catalog.dto.response.CategoryResponse;
import com.ids.ms_catalog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

	private static final String ID_PATH = "/{id}";

	private final CategoryService categoryService;

	@GetMapping
	public List<CategoryResponse> list(@RequestParam(required = false) Boolean active) {
		return categoryService.findAll(active);
	}

	@GetMapping(ID_PATH)
	public CategoryResponse get(@PathVariable Long id) {
		return categoryService.findById(id);
	}

	@PostMapping
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		CategoryResponse created = categoryService.create(request);
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
				.path(ID_PATH).buildAndExpand(created.id()).toUri()).body(created);
	}

	@PutMapping(ID_PATH)
	public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.update(id, request);
	}

	@DeleteMapping(ID_PATH)
	public ResponseEntity<Void> deactivate(@PathVariable Long id) {
		categoryService.deactivate(id);
		return ResponseEntity.noContent().build();
	}
}
