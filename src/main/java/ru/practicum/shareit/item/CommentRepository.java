package ru.practicum.shareit.item;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByItemIdOrderByCreatedAsc(Long itemId);

    void deleteByItemIdIn(List<Long> itemIds);

    void deleteByAuthorId(Long authorId);
}
