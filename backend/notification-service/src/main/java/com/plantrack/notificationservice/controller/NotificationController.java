package com.plantrack.notificationservice.controller;

import com.plantrack.notificationservice.dto.CreateNotificationRequest;
import com.plantrack.notificationservice.dto.InitiativeAssignedRequest;
import com.plantrack.notificationservice.dto.StatusUpdateRequest;
import com.plantrack.notificationservice.dto.WeeklyReportRequest;
import com.plantrack.notificationservice.model.Notification;
import com.plantrack.notificationservice.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // --- SSE Endpoint ---
    @GetMapping("/stream")
    public SseEmitter streamNotifications(@RequestParam Long userId) {
        return notificationService.subscribe(userId);
    }

    @GetMapping("/{userId}")
    public List<Notification> getAllNotifications(@PathVariable Long userId) {
        return notificationService.getAllNotifications(userId);
    }

    @GetMapping("/{userId}/unread")
    public List<Notification> getUnreadNotifications(@PathVariable Long userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/{userId}/unread-count")
    public Long getUnreadCount(@PathVariable Long userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PutMapping("/{notificationId}/read")
    public void markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
    }

    @PutMapping("/{userId}/read-all")
    public void markAllAsRead(@PathVariable Long userId) {
        notificationService.markAllAsRead(userId);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createNotification(@RequestBody CreateNotificationRequest req) {
        notificationService.createNotification(
                req.getUserId(),
                req.getType(),
                req.getMessage(),
                req.getEntityType(),
                req.getEntityId()
        );
    }

    @PostMapping("/initiative-assigned")
    @ResponseStatus(HttpStatus.CREATED)
    public void notifyInitiativeAssigned(@RequestBody InitiativeAssignedRequest req) {
        notificationService.notifyInitiativeAssigned(
                req.getEmployeeUserId(),
                req.getInitiativeTitle(),
                req.getInitiativeId()
        );
    }

    @PostMapping("/status-update")
    @ResponseStatus(HttpStatus.CREATED)
    public void notifyStatusUpdate(@RequestBody StatusUpdateRequest req) {
        notificationService.notifyStatusUpdate(
                req.getManagerUserId(),
                req.getEmployeeName(),
                req.getInitiativeTitle(),
                req.getNewStatus(),
                req.getInitiativeId()
        );
    }

    @PostMapping("/weekly-report")
    @ResponseStatus(HttpStatus.CREATED)
    public void notifyWeeklyReport(@RequestBody WeeklyReportRequest req) {
        notificationService.notifyWeeklyReport(
                req.getAdminUserId(),
                req.getReportSummary()
        );
    }
}
