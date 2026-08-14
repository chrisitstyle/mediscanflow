package com.chrisitstyle.mediscanflow.medicalplatform.users.audit;

import com.chrisitstyle.mediscanflow.medicalplatform.audit.AuditMetadata;
import com.chrisitstyle.mediscanflow.medicalplatform.users.dto.UserStatusDTO;

/**
 * Metadata describing a user account status change.
 *
 * @param targetUserId identifier of the affected user
 * @param targetUserEmail email address of the affected user
 * @param status resulting status of the user account
 */
public record UserStatusChangedAuditMetadata(
        String targetUserId,
        String targetUserEmail,
        UserStatusDTO status) implements AuditMetadata { }
