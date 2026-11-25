package ru.practicum.shareit.item;

import java.util.List;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;

public interface ItemService {
    ItemDto create(long userId, ItemDto itemDto);

    ItemDto update(long userId, long itemId, ItemDto itemDto);

    ItemResponseDto getById(long userId, long itemId);

    List<ItemResponseDto> getByOwner(long userId);

    List<ItemDto> search(String text);
}


