package ru.practicum.shareit.user;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.CommentRepository;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.dto.UserDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @InjectMocks
    private UserServiceImpl userService;

    private User existing;

    @BeforeEach
    void setUp() {
        existing = new User();
        existing.setId(1L);
        existing.setName("Иван");
        existing.setEmail("ivan@mail.com");
    }

    @Test
    void createSavesNewUser() {
        when(userRepository.existsByEmail("ivan@mail.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        UserDto result = userService.create(dto("Иван", "ivan@mail.com"));

        assertThat(result.getId(), is(1L));
        assertThat(result.getName(), is("Иван"));
        assertThat(result.getEmail(), is("ivan@mail.com"));
    }

    @Test
    void createWithDuplicateEmailThrows() {
        when(userRepository.existsByEmail("ivan@mail.com")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.create(dto("Иван", "ivan@mail.com")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateChangesNameAndEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("petr@mail.com", 1L)).thenReturn(false);
        when(userRepository.save(existing)).thenReturn(existing);

        UserDto result = userService.update(1L, dto("Пётр", "petr@mail.com"));

        assertThat(result.getName(), is("Пётр"));
        assertThat(result.getEmail(), is("petr@mail.com"));
    }

    @Test
    void updateKeepsFieldsWhenTheyAreNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);

        UserDto result = userService.update(1L, dto(null, null));

        assertThat(result.getName(), is("Иван"));
        assertThat(result.getEmail(), is("ivan@mail.com"));
        verify(userRepository, never()).existsByEmailAndIdNot(any(), anyLong());
    }

    @Test
    void updateWithDuplicateEmailThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("busy@mail.com", 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.update(1L, dto(null, "busy@mail.com")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateMissingUserThrows() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.update(99L, dto("Пётр", null)));
    }

    @Test
    void getByIdReturnsUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserDto result = userService.getById(1L);

        assertThat(result.getId(), is(1L));
        assertThat(result.getEmail(), is("ivan@mail.com"));
    }

    @Test
    void getMissingUserThrows() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.getById(99L));
    }

    @Test
    void deleteRemovesUserWithItems() {
        Item item = new Item();
        item.setId(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));

        userService.delete(1L);

        verify(commentRepository).deleteByItemIdIn(List.of(10L));
        verify(bookingRepository).deleteByItemIdIn(List.of(10L));
        verify(commentRepository).deleteByAuthorId(1L);
        verify(bookingRepository).deleteByBookerId(1L);
        verify(itemRepository).deleteByOwnerId(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteSkipsItemCleanupWhenUserHasNoItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of());

        userService.delete(1L);

        verify(commentRepository, never()).deleteByItemIdIn(any());
        verify(bookingRepository, never()).deleteByItemIdIn(any());
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteMissingUserThrows() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> userService.delete(99L));
        verify(userRepository, never()).deleteById(anyLong());
    }

    private UserDto dto(String name, String email) {
        UserDto dto = new UserDto();
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }
}
