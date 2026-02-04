package com.plantrack.notificationservice.service;

import com.plantrack.notificationservice.model.Notification;
import com.plantrack.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    // Thread-safe storage for active SSE connections
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // --- SSE Logic ---

    public SseEmitter subscribe(Long userId) {
        // 1 Hour Timeout (3600000ms)
        SseEmitter emitter = new SseEmitter(3600000L);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // Remove emitter on completion, timeout, or error
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((e) -> removeEmitter(userId, emitter));

        return emitter;
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    private void pushNotificationToUser(Long userId, Notification notification) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            emitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("notification")
                            .data(notification));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            });
            emitters.removeAll(deadEmitters);
        }
    }

    // --- Main Logic ---

    public void createNotification(Long userId, String type, String message) {
        createNotification(userId, type, message, null, null);
    }

    public void createNotification(Long userId, String type, String message, String entityType, Long entityId) {
        try {
            Notification notification = new Notification();
            notification.setUser(userId);
            notification.setType(type);
            notification.setMessage(message);
            notification.setEntityType(entityType);
            notification.setEntityId(entityId);
            notification.setStatus("UNREAD");
            notification.setCreatedDate(LocalDateTime.now());

            Notification saved = notificationRepository.save(notification);

            // PUSH Real-time update
            pushNotificationToUser(userId, saved);

            logger.info("Notification created and pushed: id={}, userId={}", saved.getNotificationId(), userId);
        } catch (Exception e) {
            logger.error("Failed to create notification", e);
            throw e;
        }
    }

    public void notifyInitiativeAssigned(Long employeeUserId, String initiativeTitle, Long initiativeId) {
        createNotification(employeeUserId, "ASSIGNMENT", "You have been assigned to: " + initiativeTitle, "INITIATIVE", initiativeId);
    }

    public void notifyStatusUpdate(Long managerUserId, String employeeName, String initiativeTitle, String newStatus, Long initiativeId) {
        createNotification(managerUserId, "STATUS_UPDATE", employeeName + " updated '" + initiativeTitle + "' to " + newStatus, "INITIATIVE", initiativeId);
    }

    public void notifyWeeklyReport(Long adminUserId, String reportSummary) {
        createNotification(adminUserId, "WEEKLY_REPORT", "Weekly Analytics Report: " + reportSummary, "SYSTEM", null);
    }

    // --- Getters & Helpers ---

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserAndStatus(userId, "UNREAD");
    }

    public List<Notification> getAllNotifications(Long userId) {
        return notificationRepository.findByUserOrderByCreatedDateDesc(userId);
    }

    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus("READ");
        notificationRepository.save(notification);
    }

    public void markAllAsRead(Long userId) {
        List<Notification> unread = getUnreadNotifications(userId);
        unread.forEach(n -> {
            n.setStatus("READ");
            notificationRepository.save(n);
        });
    }
}