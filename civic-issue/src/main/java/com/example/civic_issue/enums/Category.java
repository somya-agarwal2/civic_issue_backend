package com.example.civic_issue.enums;

public enum Category {
    WATER_SEWERAGE("Water & Sewerage", "Water & Sewerage"),
    ELECTRICITY_POWER("Electricity & Power", "Electricity & Power"),
    ROADS_TRANSPORT("Roads & Transport", "Roads & Transport"),
    SANITATION_WASTE("Sanitation & Waste", "Sanitation & Waste"),
    HEALTH_PUBLIC_SAFETY("Health & Public Safety", "Health & Public Safety");

    private final String displayName;
    private final String departmentName;

    Category(String displayName, String departmentName) {
        this.displayName = displayName;
        this.departmentName = departmentName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    // ✅ Add this static method
    public static Category fromString(String value) {
        if (value == null) return null;
        for (Category c : Category.values()) {
            if (c.displayName.equalsIgnoreCase(value.trim())) {
                return c;
            }
        }
        return null; // invalid category
    }
}

