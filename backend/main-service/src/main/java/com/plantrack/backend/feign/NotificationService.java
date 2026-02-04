package com.plantrack.backend.feign;

import com.plantrack.backend.dto.CreateNotificationRequest;
import com.plantrack.backend.dto.InitiativeAssignedRequest;
import com.plantrack.backend.dto.StatusUpdateRequest;
import com.plantrack.backend.dto.WeeklyReportRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@FeignClient("notification-service")
public interface NotificationService {
    @PostMapping("/api/notifications/create")
    @ResponseStatus(HttpStatus.CREATED)
    public void createNotification(@RequestBody CreateNotificationRequest req);

    @PostMapping("/api/notifications/initiative-assigned")
    @ResponseStatus(HttpStatus.CREATED)
    public void notifyInitiativeAssigned(@RequestBody InitiativeAssignedRequest req);

    @PostMapping("/api/notifications/status-update")
    @ResponseStatus(HttpStatus.CREATED)
    public void notifyStatusUpdate(@RequestBody StatusUpdateRequest req);

    @PostMapping("/api/notifications/weekly-report")
    @ResponseStatus(HttpStatus.CREATED)
    public void notifyWeeklyReport(@RequestBody WeeklyReportRequest req);
}
