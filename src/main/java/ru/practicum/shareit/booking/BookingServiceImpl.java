package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "start");

    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    public BookingDto create(long userId, BookingRequestDto requestDto) {
        User booker = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Item item = itemRepository.findById(requestDto.getItemId())
                .orElseThrow(() -> new NotFoundException("Вещь с id " + requestDto.getItemId() + " не найдена"));
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new BadRequestException("Вещь недоступна для бронирования");
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new BadRequestException("Нельзя бронировать собственную вещь");
        }
        if (!requestDto.getStart().isBefore(requestDto.getEnd())) {
            throw new BadRequestException("Дата начала должна быть раньше даты окончания");
        }
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(requestDto.getStart());
        booking.setEnd(requestDto.getEnd());
        booking.setStatus(BookingStatus.WAITING);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto approve(long userId, long bookingId, boolean approved) {
        Booking booking = findBooking(bookingId);
        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Подтвердить бронирование может только владелец вещи");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BadRequestException("Бронирование уже обработано");
        }
        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingDto getById(long userId, long bookingId) {
        Booking booking = findBooking(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new ForbiddenException("Просмотр бронирования доступен только автору и владельцу вещи");
        }
        return BookingMapper.toBookingDto(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getByBooker(long userId, BookingState state) {
        requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByBookerId(userId, NEWEST_FIRST);
            case CURRENT ->
                    bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(userId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByBookerIdAndEndBefore(userId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByBookerIdAndStartAfter(userId, now, NEWEST_FIRST);
            case WAITING, REJECTED ->
                    bookingRepository.findByBookerIdAndStatus(userId, BookingStatus.valueOf(state.name()), NEWEST_FIRST);
        };
        return bookings.stream().map(BookingMapper::toBookingDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> getByOwner(long userId, BookingState state) {
        requireUser(userId);
        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = switch (state) {
            case ALL -> bookingRepository.findByItemOwnerId(userId, NEWEST_FIRST);
            case CURRENT ->
                    bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(userId, now, now, NEWEST_FIRST);
            case PAST -> bookingRepository.findByItemOwnerIdAndEndBefore(userId, now, NEWEST_FIRST);
            case FUTURE -> bookingRepository.findByItemOwnerIdAndStartAfter(userId, now, NEWEST_FIRST);
            case WAITING, REJECTED ->
                    bookingRepository.findByItemOwnerIdAndStatus(userId, BookingStatus.valueOf(state.name()), NEWEST_FIRST);
        };
        return bookings.stream().map(BookingMapper::toBookingDto).toList();
    }

    private Booking findBooking(long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Бронирование с id " + bookingId + " не найдено"));
    }

    private void requireUser(long userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }
}
