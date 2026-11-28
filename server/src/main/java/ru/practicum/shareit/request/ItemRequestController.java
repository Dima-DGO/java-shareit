package ru.practicum.shareit.request;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.util.Constants;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class ItemRequestController {
    private final ItemRequestService itemRequestService;

    @PostMapping
    public ItemRequestDto create(@RequestHeader(Constants.USER_HEADER) long userId,
                                 @RequestBody ItemRequestDto dto) {
        return itemRequestService.create(userId, dto);
    }

    @GetMapping
    public List<ItemRequestDto> getOwn(@RequestHeader(Constants.USER_HEADER) long userId) {
        return itemRequestService.getOwn(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestDto> getOthers(@RequestHeader(Constants.USER_HEADER) long userId) {
        return itemRequestService.getOthers(userId);
    }

    @GetMapping("/{requestId}")
    public ItemRequestDto getById(@RequestHeader(Constants.USER_HEADER) long userId,
                                  @PathVariable long requestId) {
        return itemRequestService.getById(userId, requestId);
    }
}
