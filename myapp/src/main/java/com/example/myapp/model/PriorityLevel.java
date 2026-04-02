package com.example.myapp.model;

public enum PriorityLevel {

    CRITICAL("Immediate life-saving intervention required", 1),
    HIGH("Urgent care needed within 15 minutes", 2),
    MEDIUM("Semi-urgent, seen within 30-60 minutes", 3),
    LOW("Non-urgent, can wait over 60 minutes", 4);

    private final String description;
    private final int order;

    PriorityLevel(String description, int order) {
        this.description = description;
        this.order = order;
    }

    public String getDescription() {
        return description;
    }

    public int getOrder() {
        return order;
    }
}
