package ru.practicum.shareit.item;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.shareit.booking.BookingService;
import ru.practicum.shareit.booking.dto.BookingRequestDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.user.UserService;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemServiceIntegrationTest {

    @Autowired
    private ItemService itemService;
    @Autowired
    private BookingService bookingService;
    @Autowired
    private UserService userService;

    @Test
    void ownerSeesLastBookingAndComments() {
        UserDto owner = userService.create(user("Владелец", "owner@mail.com"));
        UserDto booker = userService.create(user("Арендатор", "booker@mail.com"));
        ItemDto created = itemService.create(owner.getId(), itemDto("Дрель"));

        Long bookingId = bookingService.create(booker.getId(), request(created.getId(), -5, -2)).getId();
        bookingService.approve(owner.getId(), bookingId, true);

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Отличная дрель");
        itemService.addComment(booker.getId(), created.getId(), commentDto);

        ItemResponseDto ownerView = itemService.getById(owner.getId(), created.getId());
        ItemResponseDto bookerView = itemService.getById(booker.getId(), created.getId());

        assertThat(ownerView.getLastBooking()).isNotNull();
        assertThat(ownerView.getNextBooking()).isNull();
        assertThat(ownerView.getComments()).hasSize(1);
        assertThat(ownerView.getRequestId()).isNull();
        assertThat(bookerView.getLastBooking()).isNull();
        assertThat(bookerView.getComments()).hasSize(1);
    }

    @Test
    void getByOwnerAndUpdateCheckOwnership() {
        UserDto owner = userService.create(user("Владелец", "owner@mail.com"));
        UserDto stranger = userService.create(user("Чужой", "stranger@mail.com"));
        ItemDto created = itemService.create(owner.getId(), itemDto("Дрель"));

        assertThat(itemService.getByOwner(owner.getId())).hasSize(1);
        assertThat(itemService.search("дрель")).hasSize(1);

        ItemDto update = new ItemDto();
        update.setAvailable(false);

        assertThatThrownBy(() -> itemService.update(stranger.getId(), created.getId(), update))
                .isInstanceOf(ForbiddenException.class);

        assertThat(itemService.update(owner.getId(), created.getId(), update).getAvailable()).isFalse();
    }

    @Test
    void addCommentWithoutRentalThrows() {
        UserDto owner = userService.create(user("Владелец", "owner@mail.com"));
        UserDto stranger = userService.create(user("Чужой", "stranger@mail.com"));
        ItemDto created = itemService.create(owner.getId(), itemDto("Дрель"));

        CommentDto commentDto = new CommentDto();
        commentDto.setText("Хочу оставить отзыв");

        assertThatThrownBy(() -> itemService.addComment(stranger.getId(), created.getId(), commentDto))
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
