package com.codeguardian.domain.model;

/**
 * Domain representation of the statuses a code review can transition through.
 */
public enum ReviewStatus {
    PENDING,
    COMPLETED,
    FAILED
}
