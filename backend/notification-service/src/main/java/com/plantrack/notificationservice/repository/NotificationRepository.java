package com.plantrack.notificationservice.repository;

import com.plantrack.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserAndStatus(Long userId, String status);

    List<Notification> findByUserOrderByCreatedDateDesc(Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :userId AND n.status = 'UNREAD'")
    Long countUnreadByUserId(@Param("userId") Long userId);
}