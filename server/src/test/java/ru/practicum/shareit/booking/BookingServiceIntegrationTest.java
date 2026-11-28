package ru.practicum.shareit.booking;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingServiceIntegrationTest {

    @Autowired
    private BookingService bookingService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private UserService userService;

    @Test
    void createApproveAndReadBooking() {
        UserDto owner = userService.create(user("Владелец", "owner@mail.com"));
        UserDto booker = userService.create(user("Арендатор", "booker@mail.com"));
        UserDto stranger = userService.create(user("Чужой", "stranger@mail.com"));
        Long itemId = itemService.create(owner.getId(), itemDto("Дрель")).getId();

        BookingDto created = bookingService.create(booker.getId(), request(itemId, 1, 3));

        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(bookingService.getById(booker.getId(), created.getId()).getId()).isEqualTo(created.getId());
        assertThat(bookingService.getById(owner.getId(), created.getId()).getId()).isEqualTo(created.getId());

        assertThatThrownBy(() -> bookingService.getById(stranger.getId(), created.getId()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> bookingService.approve(booker.getId(), created.getId(), true))
                .isInstanceOf(ForbiddenException.class);

        BookingDto approved = bookingService.approve(owner.getId(), created.getId(), true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThatThrownBy(() -> bookingService.approve(owner.getId(), created.getId(), false))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void listsFilterByState() {
        UserDto owner = userService.create(user("Владелец", "owner@mail.com"));
        UserDto booker = userService.create(user("Арендатор", "booker@mail.com"));
        Long itemId = itemService.create(owner.getId(), itemDto("Дрель")).getId();

        bookingService.create(booker.getId(), request(itemId, -5, -2));
        bookingService.create(booker.getId(), request(itemId, 2, 5));

        assertThat(bookingService.getByBooker(booker.getId(), BookingState.ALL)).hasSize(2);
        assertThat(bookingService.getByBooker(booker.getId(), BookingState.PAST)).hasSize(1);
        assertThat(bookingService.getByBooker(booker.getId(), BookingState.FUTURE)).hasSize(1);
        assertThat(bookingService.getByOwner(owner.getId(), BookingState.ALL)).hasSize(2);
        assertThat(bookingService.getByOwner(owner.getId(), BookingState.WAITING)).hasSize(2);
    }

    @Test
    void createRejectsUnavailableItem() {
        UserDto owner = userService.create(user("Владелец", "owner@mail.com"));
        UserDto booker = userService.create(user("Арендатор", "booker@mail.com"));

        ItemDto unavailable = itemDto("Дрель");
        unavailable.setAvailable(false);
        Long itemId = itemService.create(owner.getId(), unavailable).getId();

        assertThatThrownBy(() -> bookingService.create(booker.getId(), request(itemId, 1, 3)))
                .isInstanceOf(BadRequestException.class);
    }

    private UserDto user(String name, String email) {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }

    private ItemDto itemDto(String name) {
        ItemDto dto = new ItemDto();
        dto.setName(name);
        dto.setDescription("Мощная");
        dto.setAvailable(true);
        return dto;
    }

    private BookingRequestDto request(Long itemId, int startDays, int endDays) {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setItemId(itemId);
        dto.setStart(LocalDateTime.now().plusDays(startDays));
        dto.setEnd(LocalDateTime.now().plusDays(endDays));
        return dto;
    }
}
