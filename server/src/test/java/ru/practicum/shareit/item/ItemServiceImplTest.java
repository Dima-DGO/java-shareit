package ru.practicum.shareit.item;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.ItemRequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    private Item item;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Иван");
        owner.setEmail("ivan@mail.com");

        item = new Item();
        item.setId(1L);
        item.setName("Дрель");
        item.setDescription("Мощная");
        item.setAvailable(true);
        item.setOwner(owner);
    }

    @Test
    void createWithoutRequestId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.existsByOwnerIdAndName(1L, "Дрель")).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> {
            Item saved = invocation.getArgument(0);
            saved.setId(28L);
            return saved;
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
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ItemDto result = itemService.create(1L, itemDto(2L));

        assertThat(result.getRequestId(), is(2L));
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

    @Test
    void updateChangesAllFields() {
        ItemDto dto = new ItemDto();
        dto.setName("Перфоратор");
        dto.setDescription("Тяжёлый");
        dto.setAvailable(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.existsByOwnerIdAndNameAndIdNot(1L, "Перфоратор", 1L)).thenReturn(false);
        when(itemRepository.save(item)).thenReturn(item);

        ItemDto result = itemService.update(1L, 1L, dto);

        assertThat(result.getName(), is("Перфоратор"));
        assertThat(result.getDescription(), is("Тяжёлый"));
        assertThat(result.getAvailable(), is(false));
    }

    @Test
    void updateKeepsFieldsWhenTheyAreNull() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        ItemDto result = itemService.update(1L, 1L, new ItemDto());

        assertThat(result.getName(), is("Дрель"));
        assertThat(result.getAvailable(), is(true));
        verify(itemRepository, never()).existsByOwnerIdAndNameAndIdNot(any(), any(), any());
    }

    @Test
    void updateWithDuplicateNameThrows() {
        ItemDto dto = new ItemDto();
        dto.setName("Перфоратор");

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.existsByOwnerIdAndNameAndIdNot(1L, "Перфоратор", 1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> itemService.update(1L, 1L, dto));
    }

    @Test
    void updateByStrangerThrows() {
        User stranger = new User();
        stranger.setId(9L);

        when(userRepository.findById(9L)).thenReturn(Optional.of(stranger));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(ForbiddenException.class, () -> itemService.update(9L, 1L, new ItemDto()));
    }

    @Test
    void updateWithUnknownUserThrows() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.update(9L, 1L, new ItemDto()));
    }

    @Test
    void updateWithUnknownItemThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.update(1L, 5L, new ItemDto()));
    }

    @Test
    void getByIdForOwnerFillsBookingsAndComments() {
        Booking past = booking(11L, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(2));
        Booking future = booking(12L, LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(5));

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.findByItemIdAndStatus(1L, BookingStatus.APPROVED))
                .thenReturn(List.of(past, future));
        when(commentRepository.findByItemIdOrderByCreatedAsc(1L)).thenReturn(List.of(comment()));

        ItemResponseDto result = itemService.getById(1L, 1L);

        assertThat(result.getLastBooking().getId(), is(11L));
        assertThat(result.getNextBooking().getId(), is(12L));
        assertThat(result.getComments(), hasSize(1));
    }

    @Test
    void getByIdForBookerHidesBookings() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(commentRepository.findByItemIdOrderByCreatedAsc(1L)).thenReturn(List.of());

        ItemResponseDto result = itemService.getById(3L, 1L);

        assertThat(result.getLastBooking(), is(nullValue()));
        assertThat(result.getNextBooking(), is(nullValue()));
        assertThat(result.getComments(), hasSize(0));
        verify(bookingRepository, never()).findByItemIdAndStatus(any(), any());
    }

    @Test
    void getByIdWithUnknownItemThrows() {
        when(itemRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.getById(1L, 5L));
    }

    @Test
    void getByOwnerReturnsItemsWithBookingsAndComments() {
        Booking past = booking(11L, LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(2));

        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of(item));
        when(bookingRepository.findByItemInAndStatus(anyList(), eq(BookingStatus.APPROVED)))
                .thenReturn(List.of(past));
        when(commentRepository.findByItemIn(anyList(), any(Sort.class))).thenReturn(List.of(comment()));

        List<ItemResponseDto> result = itemService.getByOwner(1L);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getLastBooking().getId(), is(11L));
        assertThat(result.get(0).getNextBooking(), is(nullValue()));
        assertThat(result.get(0).getComments(), hasSize(1));
    }

    @Test
    void getByOwnerReturnsEmptyListWhenNoItems() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findByOwnerId(1L)).thenReturn(List.of());

        List<ItemResponseDto> result = itemService.getByOwner(1L);

        assertThat(result, hasSize(0));
        verify(bookingRepository, never()).findByItemInAndStatus(anyList(), any());
    }

    @Test
    void getByOwnerForUnknownUserThrows() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.getByOwner(9L));
    }

    @Test
    void searchReturnsItems() {
        when(itemRepository.search("дрель")).thenReturn(List.of(item));

        List<ItemResponseDto> result = itemService.search("дрель");

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getName(), is("Дрель"));
    }

    @Test
    void searchWithBlankTextReturnsEmptyList() {
        assertThat(itemService.search("   "), hasSize(0));
        assertThat(itemService.search(null), hasSize(0));
        verify(itemRepository, never()).search(any());
    }

    @Test
    void addCommentSavesComment() {
        CommentDto dto = new CommentDto();
        dto.setText("Отличная дрель");

        when(userRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                eq(1L), eq(3L), eq(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(true);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        CommentDto result = itemService.addComment(3L, 1L, dto);

        assertThat(result.getId(), is(7L));
        assertThat(result.getText(), is("Отличная дрель"));
        assertThat(result.getAuthorName(), is("Иван"));
    }

    @Test
    void addCommentWithoutCompletedRentalThrows() {
        CommentDto dto = new CommentDto();
        dto.setText("Отличная дрель");

        when(userRepository.findById(3L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                eq(1L), eq(3L), eq(BookingStatus.APPROVED), any(LocalDateTime.class))).thenReturn(false);

        assertThrows(BadRequestException.class, () -> itemService.addComment(3L, 1L, dto));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addCommentWithUnknownUserThrows() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.addComment(9L, 1L, new CommentDto()));
    }

    @Test
    void addCommentWithUnknownItemThrows() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(itemRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> itemService.addComment(1L, 5L, new CommentDto()));
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

    private Booking booking(long id, LocalDateTime start, LocalDateTime end) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setItem(item);
        booking.setBooker(owner);
        booking.setStart(start);
        booking.setEnd(end);
        booking.setStatus(BookingStatus.APPROVED);
        return booking;
    }

    private Comment comment() {
        Comment comment = new Comment();
        comment.setId(7L);
        comment.setText("Отличная дрель");
        comment.setItem(item);
        comment.setAuthor(owner);
        comment.setCreated(LocalDateTime.now());
        return comment;
    }
}
