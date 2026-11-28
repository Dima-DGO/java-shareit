package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private BookingServiceImpl bookingService;

    private User owner;
    private User booker;
    private Item item;

    @BeforeEach
    void setUp() {
        owner = user(1L, "Владелец");
        booker = user(3L, "Арендатор");

        item = new Item();
        item.setId(2L);
        item.setName("Дрель");
        item.setOwner(owner);
        item.setAvailable(true);
    }

    @Test
    void createSavesWaitingBooking() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(10L);
            return booking;
        });

        BookingDto result = bookingService.create(3L, request(1, 2));

        assertThat(result.getId(), is(10L));
        assertThat(result.getStatus(), is(BookingStatus.WAITING));
        assertThat(result.getBooker().getId(), is(3L));
        assertThat(result.getItem().getId(), is(2L));
    }

    @Test
    void createWithUnknownUserThrows() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(99L, request(1, 2)));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createWithUnknownItemThrows() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.create(3L, request(1, 2)));
    }

    @Test
    void createWithUnavailableItemThrows() {
        item.setAvailable(false);
        when(userRepository.findById(3L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> bookingService.create(3L, request(1, 2)));
    }

    @Test
    void createOwnItemThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        assertThrows(BadRequestException.class, () -> bookingService.create(1L, request(1, 2)));
    }

    @Test
    void createWithStartAfterEndThrows() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(booker));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        BookingRequestDto dto = request(5, 2);

        assertThrows(BadRequestException.class, () -> bookingService.create(3L, dto));
    }

    @Test
    void approveSetsApproved() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDto result = bookingService.approve(1L, 10L, true);

        assertThat(result.getStatus(), is(BookingStatus.APPROVED));
    }

    @Test
    void approveSetsRejected() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking()));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingDto result = bookingService.approve(1L, 10L, false);

        assertThat(result.getStatus(), is(BookingStatus.REJECTED));
    }

    @Test
    void approveByStrangerThrows() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking()));

        assertThrows(ForbiddenException.class, () -> bookingService.approve(9L, 10L, true));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void approveAlreadyProcessedThrows() {
        Booking booking = booking();
        booking.setStatus(BookingStatus.APPROVED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.approve(1L, 10L, true));
    }

    @Test
    void approveMissingBookingThrows() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.approve(1L, 10L, true));
    }

    @Test
    void getByIdAvailableForBooker() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking()));

        assertThat(bookingService.getById(3L, 10L).getId(), is(10L));
    }

    @Test
    void getByIdAvailableForOwner() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking()));

        assertThat(bookingService.getById(1L, 10L).getId(), is(10L));
    }

    @Test
    void getByIdForStrangerThrows() {
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking()));

        assertThrows(ForbiddenException.class, () -> bookingService.getById(9L, 10L));
    }

    @Test
    void getByBookerCoversAllStates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(booker));
        when(bookingRepository.findByBookerId(eq(1L), any(Sort.class))).thenReturn(List.of(booking()));
        when(bookingRepository.findByBookerIdAndStartBeforeAndEndAfter(eq(1L), any(), any(), any(Sort.class)))
                .thenReturn(List.of(booking()));
        when(bookingRepository.findByBookerIdAndEndBefore(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of(booking()));
        when(bookingRepository.findByBookerIdAndStartAfter(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of());
        when(bookingRepository.findByBookerIdAndStatus(eq(1L), eq(BookingStatus.WAITING), any(Sort.class)))
                .thenReturn(List.of(booking()));
        when(bookingRepository.findByBookerIdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any(Sort.class)))
                .thenReturn(List.of());

        assertThat(bookingService.getByBooker(1L, BookingState.ALL), hasSize(1));
        assertThat(bookingService.getByBooker(1L, BookingState.CURRENT), hasSize(1));
        assertThat(bookingService.getByBooker(1L, BookingState.PAST), hasSize(1));
        assertThat(bookingService.getByBooker(1L, BookingState.FUTURE), hasSize(0));
        assertThat(bookingService.getByBooker(1L, BookingState.WAITING), hasSize(1));
        assertThat(bookingService.getByBooker(1L, BookingState.REJECTED), hasSize(0));
    }

    @Test
    void getByOwnerCoversAllStates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByItemOwnerId(eq(1L), any(Sort.class))).thenReturn(List.of(booking()));
        when(bookingRepository.findByItemOwnerIdAndStartBeforeAndEndAfter(eq(1L), any(), any(), any(Sort.class)))
                .thenReturn(List.of(booking()));
        when(bookingRepository.findByItemOwnerIdAndEndBefore(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of());
        when(bookingRepository.findByItemOwnerIdAndStartAfter(eq(1L), any(), any(Sort.class)))
                .thenReturn(List.of(booking()));
        when(bookingRepository.findByItemOwnerIdAndStatus(eq(1L), eq(BookingStatus.WAITING), any(Sort.class)))
                .thenReturn(List.of(booking()));
        when(bookingRepository.findByItemOwnerIdAndStatus(eq(1L), eq(BookingStatus.REJECTED), any(Sort.class)))
                .thenReturn(List.of(booking()));

        assertThat(bookingService.getByOwner(1L, BookingState.ALL), hasSize(1));
        assertThat(bookingService.getByOwner(1L, BookingState.CURRENT), hasSize(1));
        assertThat(bookingService.getByOwner(1L, BookingState.PAST), hasSize(0));
        assertThat(bookingService.getByOwner(1L, BookingState.FUTURE), hasSize(1));
        assertThat(bookingService.getByOwner(1L, BookingState.WAITING), hasSize(1));
        assertThat(bookingService.getByOwner(1L, BookingState.REJECTED), hasSize(1));
    }

    @Test
    void getByBookerForUnknownUserThrows() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.getByBooker(99L, BookingState.ALL));
    }

    @Test
    void getByOwnerForUnknownUserThrows() {
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.getByOwner(99L, BookingState.ALL));
    }

    private Booking booking() {
        Booking booking = new Booking();
        booking.setId(10L);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(LocalDateTime.now().plusDays(1));
        booking.setEnd(LocalDateTime.now().plusDays(3));
        booking.setStatus(BookingStatus.WAITING);
        return booking;
    }

    private BookingRequestDto request(int startDays, int endDays) {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setItemId(2L);
        dto.setStart(LocalDateTime.now().plusDays(startDays));
        dto.setEnd(LocalDateTime.now().plusDays(endDays));
        return dto;
    }

    private User user(long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(name.toLowerCase() + "@mail.ru");
        return user;
    }
}
