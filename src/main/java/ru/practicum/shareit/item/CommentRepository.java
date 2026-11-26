package ru.practicum.shareit.item;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.shareit.item.model.Item;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByItemIdOrderByCreatedAsc(Long itemId);

    void deleteByItemIdIn(List<Long> itemIds);

    void deleteByAuthorId(Long authorId);

    List<Comment> findByItemIn(List<Item> items, Sort sort);
}
