package com.finance.tracker.repository;

import com.finance.tracker.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    void deleteByUserId(Long userId);
    List<Notification> findByUserIdAndIsReadFalse(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
    java.util.Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllReadByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(n) > 0 FROM Notification n WHERE n.user.id = :userId AND n.title = :title AND n.message LIKE %:messageFragment%")
    boolean existsByUserIdAndTitleAndMessageContaining(@Param("userId") Long userId, @Param("title") String title, @Param("messageFragment") String messageFragment);
}
