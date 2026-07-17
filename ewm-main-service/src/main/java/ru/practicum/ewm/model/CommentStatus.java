package ru.practicum.ewm.model;

public enum CommentStatus {
    PENDING,    // Ожидает модерации
    PUBLISHED,  // Опубликован
    REJECTED,   // Отклонён модератором
    DELETED     // Удалён (мягкое удаление)
}