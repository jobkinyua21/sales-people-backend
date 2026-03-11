package com.possystem.kitchen.production;

public enum ProductionStatus {
    PENDING,        // Created but not started
    IN_PROGRESS,    // Chef is producing
    COMPLETED,      // Finished — ingredients deducted, finished goods added
    CANCELLED       // Cancelled before completion
}
