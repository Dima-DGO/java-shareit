package ru.practicum.shareit.request;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    List<ItemRequest> findByRequesterIdOrderByCreatedDesc(long requesterId);

    List<ItemRequest> findByRequesterIdNotOrderByCreatedDesc(long requesterId);
}