package ru.practicum.shareit.request;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository requestRepository;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private ItemRequestServiceImpl requestService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Иван");
        user.setEmail("ivan@mail.com");
    }

    @Test
    void createSavesRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.save(any(ItemRequest.class))).thenAnswer(invocation -> {
            ItemRequest request = invocation.getArgument(0);
            request.setId(10L);
            return request;
        });

        ItemRequestDto result = requestService.create(1L, requestDto("Нужна дрель"));

        assertThat(result.getId(), is(10L));
        assertThat(result.getDescription(), is("Нужна дрель"));
        assertThat(result.getCreated(), notNullValue());
        assertThat(result.getItems(), is(empty()));
    }

    @Test
    void createThrowsWhenUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestService.create(2L, requestDto("Нужна дрель")));

        assertThat(exception.getMessage(), containsString("2"));
        verify(requestRepository, never()).save(any());
    }

    @Test
    void getOwnReturnsRequestsWithAnswerItems() {
        ItemRequest request = request(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findByRequesterIdOrderByCreatedDesc(1L)).thenReturn(List.of(request));
        when(itemRepository.findByRequestIdIn(List.of(10L))).thenReturn(List.of(item(28L, "Дрель", request)));

        List<ItemRequestDto> result = requestService.getOwn(1L);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getItems(), hasSize(1));
        assertThat(result.get(0).getItems().get(0).getName(), is("Дрель"));
        assertThat(result.get(0).getItems().get(0).getOwnerId(), is(1L));
    }

    @Test
    void getOwnReturnsEmptyListForUserWithoutRequests() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findByRequesterIdOrderByCreatedDesc(1L)).thenReturn(List.of());

        assertThat(requestService.getOwn(1L), is(empty()));
        verifyNoInteractions(itemRepository);
    }

    @Test
    void getOthersUsesRequestedPage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findByRequesterIdNotOrderByCreatedDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of());

        requestService.getOthers(1L, 20, 10);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(requestRepository).findByRequesterIdNotOrderByCreatedDesc(eq(1L), captor.capture());
        assertThat(captor.getValue(), is(PageRequest.of(2, 10)));
    }

    @Test
    void getOthersRejectsUnknownUser() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.getOthers(5L, 0, 10));
    }

    @Test
    void getByIdReturnsRequestWithItems() {
        ItemRequest request = request(10L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(itemRepository.findByRequestIdIn(List.of(10L))).thenReturn(List.of(item(28L, "Дрель", request)));

        ItemRequestDto result = requestService.getById(1L, 10L);

        assertThat(result.getId(), is(10L));
        assertThat(result.getItems(), hasSize(1));
    }

    @Test
    void getByIdThrowsWhenRequestMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(requestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> requestService.getById(1L, 99L));
    }

    private ItemRequestDto requestDto(String description) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setDescription(description);
        return dto;
    }

    private ItemRequest request(long id) {
        ItemRequest request = new ItemRequest();
        request.setId(id);
        request.setDescription("Нужна дрель");
        request.setRequester(user);
        request.setCreated(LocalDateTime.now());
        return request;
    }

    private Item item(long id, String name, ItemRequest request) {
        Item item = new Item();
        item.setId(id);
        item.setName(name);
        item.setDescription("Мощная");
        item.setAvailable(true);
        item.setOwner(user);
        item.setRequest(request);
        return item;
    }
}
