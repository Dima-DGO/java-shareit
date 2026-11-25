package ru.practicum.shareit.booking;

import java.util.List;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

public interface BookingService {
    BookingDto create(long userId, BookingRequestDto requestDto);

    BookingDto approve(long userId, long bookingId, boolean approved);

    BookingDto getById(long userId, long bookingId);

    List<BookingDto> getByBooker(long userId, BookingState state);

    List<BookingDto> getByOwner(long userId, BookingState state);
}

