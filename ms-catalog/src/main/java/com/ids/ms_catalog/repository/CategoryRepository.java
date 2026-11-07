package com.ids.ms_catalog.repository;

import com.ids.ms_catalog.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, Long id);

	List<Category> findAllByOrderByNameAsc();

	List<Category> findAllByActiveOrderByNameAsc(boolean active);
}
