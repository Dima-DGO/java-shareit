package ru.practicum.shareit.item;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.ForbiddenException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Override
    public ItemDto create(long userId, ItemDto itemDto) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Item item = ItemMapper.toItem(itemDto);
        item.setOwner(owner);
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    public ItemDto update(long userId, long itemId, ItemDto itemDto) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        if (item.getOwner() == null || !Objects.equals(item.getOwner().getId(), userId)) {
            throw new ForbiddenException("Редактировать вещь может только её владелец");
        }
        if (itemDto.getName() != null && !itemDto.getName().isBlank()) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null && !itemDto.getDescription().isBlank()) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public ItemResponseDto getById(long userId, long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        ItemResponseDto dto = ItemMapper.toItemResponseDto(item);
        if (item.getOwner().getId().equals(userId)) {
            fillBookings(dto, itemId);
        }
        fillComments(dto, itemId);
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDto> getByOwner(long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        return itemRepository.findByOwnerId(userId).stream()
                .map(item -> {
                    ItemResponseDto dto = ItemMapper.toItemResponseDto(item);
                    fillBookings(dto, item.getId());
                    fillComments(dto, item.getId());
                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemResponseDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemResponseDto)
                .toList();
    }

    @Override
    public CommentDto addComment(long userId, long itemId, CommentDto commentDto) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Вещь с id " + itemId + " не найдена"));
        boolean rented = bookingRepository.existsByItemIdAndBookerIdAndStatusAndEndBefore(
                itemId, userId, BookingStatus.APPROVED, LocalDateTime.now());
        if (!rented) {
            throw new BadRequestException("Оставить отзыв можно только после завершения аренды");
        }
        Comment comment = new Comment();
        comment.setText(commentDto.getText());
        comment.setItem(item);
        comment.setAuthor(author);
        comment.setCreated(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return ItemMapper.toCommentDto(commentRepository.save(comment));
    }

    private void fillBookings(ItemResponseDto dto, long itemId) {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> approved = bookingRepository.findByItemIdAndStatus(itemId, BookingStatus.APPROVED);
        approved.stream()
                .filter(booking -> booking.getEnd().isBefore(now))
                .max(Comparator.comparing(Booking::getEnd))
                .ifPresent(booking -> dto.setLastBooking(toShort(booking)));
        approved.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .ifPresent(booking -> dto.setNextBooking(toShort(booking)));
    }

    private void fillComments(ItemResponseDto dto, long itemId) {
        dto.setComments(commentRepository.findByItemIdOrderByCreatedAsc(itemId).stream()
                .map(ItemMapper::toCommentDto)
                .toList());
    }

    private BookingShortDto toShort(Booking booking) {
        BookingShortDto dto = new BookingShortDto();
        dto.setId(booking.getId());
        dto.setBookerId(booking.getBooker().getId());
        return dto;
    }
}
