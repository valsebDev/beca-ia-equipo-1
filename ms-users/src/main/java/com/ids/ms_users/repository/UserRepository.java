package com.ids.ms_users.repository;

import com.ids.ms_users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUserIdentifier(String userIdentifier);

    boolean existsByEmail(String email);
}
