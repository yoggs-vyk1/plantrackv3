package com.plantrack.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeAssignedRequest {
    private Long employeeUserId;
    private String initiativeTitle;
    private Long initiativeId;
}