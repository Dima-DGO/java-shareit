package ru.practicum.shareit.booking.dto;

import java.time.LocalDateTime;

import lombok.Data;

import ru.practicum.shareit.booking.BookingStatus;

@Data
public class BookingDto {
    private Long id;
    private LocalDateTime start;
    private LocalDateTime end;
    private BookingStatus status;
    private BookerDto booker;
    private BookingItemDto item;
}
