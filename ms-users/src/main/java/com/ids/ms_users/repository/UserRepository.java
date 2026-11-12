package com.ids.ms_users.repository;

import com.ids.ms_users.domain.Role;
import com.ids.ms_users.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findByRole(Role role);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByRole(Role role);
}
