package ru.practicum.shareit.item.dto;

import lombok.Data;

import ru.practicum.shareit.booking.dto.BookingShortDto;

@Data
public class ItemResponseDto {
    private Long id;
    private String name;
    private String description;
    private Boolean available;
    private BookingShortDto lastBooking;
    private BookingShortDto nextBooking;
}
