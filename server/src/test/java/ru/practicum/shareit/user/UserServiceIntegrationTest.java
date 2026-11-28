package ru.practicum.shareit.user;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.item.Comment;
import ru.practicum.shareit.item.CommentRepository;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private CommentRepository commentRepository;

    @Test
    void createUpdateAndReadUser() {
        UserDto created = userService.create(dto("Иван", "ivan@mail.com"));

        UserDto update = new UserDto();
        update.setName("Пётр");
        UserDto updated = userService.update(created.getId(), update);

        assertThat(updated.getName()).isEqualTo("Пётр");
        assertThat(updated.getEmail()).isEqualTo("ivan@mail.com");
        assertThat(userService.getById(created.getId()).getName()).isEqualTo("Пётр");
    }

    @Test
    void deleteUserRemovesItemsBookingsAndComments() {
        UserDto owner = userService.create(dto("Владелец", "owner@mail.com"));
        UserDto booker = userService.create(dto("Арендатор", "booker@mail.com"));

        Item item = itemRepository.save(item(owner.getId()));
        Booking booking = bookingRepository.save(booking(item, booker.getId()));
        commentRepository.save(comment(item, booker.getId()));

        userService.delete(owner.getId());

        assertThat(userRepository.findById(owner.getId())).isEmpty();
        assertThat(itemRepository.findById(item.getId())).isEmpty();
        assertThat(bookingRepository.findById(booking.getId())).isEmpty();

        userService.delete(booker.getId());

        assertThat(userRepository.findById(booker.getId())).isEmpty();
        assertThat(bookingRepository.findByBookerId(booker.getId(), null)).isEmpty();
    }

    private UserDto dto(String name, String email) {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }

    private Item item(Long ownerId) {
        Item item = new Item();
        item.setName("Дрель");
        item.setDescription("Мощная");
        item.setAvailable(true);
        item.setOwner(userRepository.findById(ownerId).orElseThrow());
        return item;
    }

    private Booking booking(Item item, Long bookerId) {
        Booking booking = new Booking();
        booking.setItem(item);
        booking.setBooker(userRepository.findById(bookerId).orElseThrow());
        booking.setStart(LocalDateTime.now().minusDays(5));
        booking.setEnd(LocalDateTime.now().minusDays(2));
        booking.setStatus(BookingStatus.APPROVED);
        return booking;
    }

    private Comment comment(Item item, Long authorId) {
        Comment comment = new Comment();
        comment.setText("Отличная дрель");
        comment.setItem(item);
        comment.setAuthor(userRepository.findById(authorId).orElseThrow());
        comment.setCreated(LocalDateTime.now());
        return comment;
    }
}
