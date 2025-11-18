package ru.practicum.model.enums;

public enum CommentStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DELETED;

    public static CommentStatus from(String value) {
        if (value == null) {
            return null;
        }
        try {
            return CommentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid comment status: " + value);
        }
    }
}