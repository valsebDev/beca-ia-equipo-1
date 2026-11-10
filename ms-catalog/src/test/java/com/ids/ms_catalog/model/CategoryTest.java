package com.ids.ms_catalog.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryTest {

	@Test
	void onCreateAsignaFechaDeCreacion() {
		Category category = new Category();

		category.onCreate();

		assertThat(category.getCreatedAt()).isNotNull();
	}

	@Test
	void onCreateIgualaLaFechaDeActualizacionAlaDeCreacion() {
		Category category = new Category();

		category.onCreate();

		assertThat(category.getUpdatedAt()).isEqualTo(category.getCreatedAt());
	}

	@Test
	void onUpdateRefrescaLaFechaDeActualizacion() {
		Category category = new Category();
		category.onCreate();
		Instant createdAt = category.getCreatedAt();

		category.onUpdate();

		assertThat(category.getUpdatedAt()).isAfterOrEqualTo(createdAt);
	}
}
