package com.example.final_project;

public class Task {
    private String reminderText;
    private String location;

    public Task(String reminderText, String location) {
        this.reminderText = reminderText;
        this.location = location;
    }
// Getter and setter methods for reminderText and location

    public String getReminderText() {
        return reminderText;
    }

    public void setReminderText(String reminderText) {
        this.reminderText = reminderText;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}