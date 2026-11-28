package ru.practicum.shareit.item;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ItemRequestRepository requestRepository;
    @InjectMocks
    private ItemServiceImpl itemService;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Иван");
        owner.setEmail("ivan@mail.com");
    }

    @Test
    void createWithoutRequestId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.existsByOwnerIdAndName(1L, "Дрель")).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(28L);
            return item;
        });

        ItemDto result = itemService.create(1L, itemDto(null));

        assertThat(result.getId(), is(28L));
        assertThat(result.getName(), is("Дрель"));
        assertThat(result.getAvailable(), is(true));
        assertThat(savedItem().getRequest(), is(nullValue()));
    }

    @Test
    void createWithRequestId() {
        ItemRequest request = new ItemRequest();
        request.setId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.existsByOwnerIdAndName(1L, "Дрель")).thenReturn(false);
        when(requestRepository.findById(2L)).thenReturn(Optional.of(request));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item item = invocation.getArgument(0);
            item.setId(29L);
            return item;
        });

        ItemDto result = itemService.create(1L, itemDto(2L));

        assertThat(result.getId(), is(29L));
        assertThat(savedItem().getRequest(), is(request));
    }

    @Test
    void createWithMissingRequestIdThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.existsByOwnerIdAndName(1L, "Дрель")).thenReturn(false);
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.create(1L, itemDto(99L)));

        assertThat(exception.getMessage(), notNullValue());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createWithUnknownUserThrows() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.create(2L, itemDto(null)));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createWithDuplicateNameThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.existsByOwnerIdAndName(1L, "Дрель")).thenReturn(true);

        assertThrows(ConflictException.class, () -> itemService.create(1L, itemDto(null)));
        verify(itemRepository, never()).save(any());
    }

    private ItemDto itemDto(Long requestId) {
        ItemDto dto = new ItemDto();
        dto.setName("Дрель");
        dto.setDescription("Мощная");
        dto.setAvailable(true);
        dto.setRequestId(requestId);
        return dto;
    }

    private Item savedItem() {
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(captor.capture());
        return captor.getValue();
    }
}
