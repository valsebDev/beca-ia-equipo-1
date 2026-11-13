package com.ids.ms_users.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import com.ids.ms_users.exception.InvalidCredentialsException;
import com.ids.ms_users.repository.UserRepository;

@Testcontainers
@SpringBootTest
class RefreshTokenServiceConcurrencyTest {

	private static final int ROUNDS = 20;
	private static final int CONTENDERS = 2;

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	private RefreshTokenService refreshTokenService;

	@Autowired
	private UserRepository userRepository;

	@Test
	void simultaneousRotationOfSameRefreshTokenSucceedsOnlyOnce() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(CONTENDERS);
		try {
			for (int round = 0; round < ROUNDS; round++) {
				String rawToken = refreshTokenService.issue(createUser());
				CountDownLatch start = new CountDownLatch(1);
				List<Future<Boolean>> results = new ArrayList<>();
				for (int i = 0; i < CONTENDERS; i++) {
					results.add(pool.submit(rotateWhenStarted(rawToken, start)));
				}
				start.countDown();

				long successes = 0;
				for (Future<Boolean> result : results) {
					if (result.get()) {
						successes++;
					}
				}
				assertThat(successes).as("ronda %d", round).isEqualTo(1);
			}
		} finally {
			pool.shutdownNow();
		}
	}

	private Callable<Boolean> rotateWhenStarted(String rawToken, CountDownLatch start) {
		return () -> {
			start.await();
			try {
				refreshTokenService.rotate(rawToken);
				return true;
			} catch (InvalidCredentialsException rejected) {
				return false;
			}
		};
	}

	private User createUser() {
		Instant now = Instant.now();
		User user = new User(
				"Test User",
				"test-" + UUID.randomUUID() + "@example.com",
				"hashed-password",
				Role.ADMIN,
				true,
				now
		);
		return userRepository.save(user);
	}
}
