package com.plantrack.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    private Long userId;
    private String type;
    private String message;
    private String entityType;
    private Long entityId;
}