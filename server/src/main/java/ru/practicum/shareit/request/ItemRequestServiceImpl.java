package ru.practicum.shareit.request;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository requestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ItemRequestDto create(long userId, ItemRequestDto dto) {
        User requester = getUser(userId);

        ItemRequest request = new ItemRequest();
        request.setDescription(dto.getDescription());
        request.setRequester(requester);
        request.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        return ItemRequestMapper.toDto(requestRepository.save(request), List.of());
    }

    @Override
    public List<ItemRequestDto> getOwn(long userId) {
        getUser(userId);
        return toDtoList(requestRepository.findByRequesterIdOrderByCreatedDesc(userId));
    }

    @Override
    public List<ItemRequestDto> getOthers(long userId) {
        getUser(userId);
        return toDtoList(requestRepository.findByRequesterIdNotOrderByCreatedDesc(userId));
    }

    @Override
    public ItemRequestDto getById(long userId, long requestId) {
        getUser(userId);

        ItemRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос с id " + requestId + " не найден"));

        return ItemRequestMapper.toDto(request, itemRepository.findByRequestIdIn(List.of(requestId)));
    }

    private User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
    }

    private List<ItemRequestDto> toDtoList(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> ids = requests.stream().map(ItemRequest::getId).toList();
        Map<Long, List<Item>> itemsByRequest = itemRepository.findByRequestIdIn(ids).stream()
                .collect(Collectors.groupingBy(item -> item.getRequest().getId()));

        return requests.stream()
                .map(request -> ItemRequestMapper.toDto(request, itemsByRequest.getOrDefault(request.getId(), List.of())))
                .toList();
    }
}
