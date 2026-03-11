package com.possystem.kitchen.recipe;

public enum ProductionType {
    COOK_TO_ORDER,  // Ingredients deducted when chef accepts KOT (e.g., steak, grilled fish)
    BATCH_PREP      // Produced in bulk, finished goods added to stock (e.g., chapati, samosa, juice)
}
