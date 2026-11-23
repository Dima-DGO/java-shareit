package ru.practicum.shareit.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import ru.practicum.shareit.item.model.Item;

@Repository
public class InMemoryItemRepository implements ItemRepository {
    private final Map<Long, Item> items = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @Override
    public Optional<Item> findById(long id) {
        return Optional.ofNullable(items.get(id));
    }

    @Override
    public List<Item> findByOwnerId(long ownerId) {
        return items.values().stream()
                .filter(item -> item.getOwner() != null && item.getOwner().getId() == ownerId)
                .toList();
    }

    @Override
    public List<Item> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public Item save(Item item) {
        if (item.getId() == null) {
            item.setId(idCounter.incrementAndGet());
        }
        items.put(item.getId(), item);
        return item;
    }

    @Override
    public void deleteByOwnerId(long ownerId) {
        items.values().removeIf(item -> item.getOwner() != null && item.getOwner().getId() == ownerId);
    }
}
