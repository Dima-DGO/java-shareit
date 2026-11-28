package ru.practicum.shareit.request;

import java.util.List;

import lombok.experimental.UtilityClass;

import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemShortDto;

@UtilityClass
public class ItemRequestMapper {

    public ItemRequestDto toDto(ItemRequest request, List<Item> items) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(request.getId());
        dto.setDescription(request.getDescription());
        dto.setCreated(request.getCreated());
        dto.setItems(items.stream().map(ItemRequestMapper::toShort).toList());
        return dto;
    }

    public ItemShortDto toShort(Item item) {
        return new ItemShortDto(item.getId(), item.getName(), item.getOwner().getId());
    }
}
