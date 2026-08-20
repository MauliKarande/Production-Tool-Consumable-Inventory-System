package com.ameya.inventory.repository;

import com.ameya.inventory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u join fetch u.role where u.username = :username")
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByRole_Name(String roleName);
}
