package com.finance.tracker.repository;

import com.finance.tracker.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Admin: search by name or email
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> searchUsers(@Param("q") String query, Pageable pageable);

    // Admin: filter by status
    Page<User> findByStatus(User.UserStatus status, Pageable pageable);

    // Admin: filter by role name
    @Query("SELECT u FROM User u WHERE u.role.roleName = :roleName")
    Page<User> findByRoleName(@Param("roleName") String roleName, Pageable pageable);

    // Monthly user growth for dashboard chart
    @Query(value = "SELECT MONTH(created_at) as month, YEAR(created_at) as year, COUNT(*) as count " +
                   "FROM users WHERE created_at >= :since GROUP BY YEAR(created_at), MONTH(created_at) " +
                   "ORDER BY YEAR(created_at), MONTH(created_at)", nativeQuery = true)
    List<Object[]> monthlyUserGrowth(@Param("since") LocalDateTime since);

    long countByStatus(User.UserStatus status);
}
