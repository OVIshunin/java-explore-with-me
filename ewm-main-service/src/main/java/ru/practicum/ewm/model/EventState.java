package ru.practicum.ewm.model;

public enum EventState {
    PENDING,    // Ожидает модерации
    PUBLISHED,  // Опубликовано
    CANCELED    // Отменено
}