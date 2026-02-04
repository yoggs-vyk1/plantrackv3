package com.plantrack.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    private Long managerUserId;
    private String employeeName;
    private String initiativeTitle;
    private String newStatus;
    private Long initiativeId;
}