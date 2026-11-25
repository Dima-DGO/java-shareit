package ru.practicum.shareit.item.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;

import lombok.Data;

@Data
public class CommentDto {
    private Long id;

    @NotBlank
    private String text;

    private String authorName;

    private LocalDateTime created;
}
