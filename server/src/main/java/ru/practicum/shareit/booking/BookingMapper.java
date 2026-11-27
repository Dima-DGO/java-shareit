package ru.practicum.shareit.booking;

import lombok.experimental.UtilityClass;

import ru.practicum.shareit.booking.dto.BookerDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingItemDto;

@UtilityClass
public class BookingMapper {

    public BookingDto toBookingDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setStart(booking.getStart());
        dto.setEnd(booking.getEnd());
        dto.setStatus(booking.getStatus());

        BookerDto booker = new BookerDto();
        booker.setId(booking.getBooker().getId());
        booker.setName(booking.getBooker().getName());
        dto.setBooker(booker);

        BookingItemDto item = new BookingItemDto();
        item.setId(booking.getItem().getId());
        item.setName(booking.getItem().getName());
        dto.setItem(item);
        return dto;
    }
}
