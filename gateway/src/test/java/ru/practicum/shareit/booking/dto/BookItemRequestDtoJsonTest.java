package ru.practicum.shareit.booking.dto;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<BookItemRequestDto> json;

    @Test
    void deserializesRequestWithoutItemId() throws Exception {
        BookItemRequestDto dto = json.parseObject(
                "{\"start\":\"2030-01-01T10:00:00\",\"end\":\"2030-01-02T10:00:00\"}");

        assertThat(dto.getItemId()).isNull();
        assertThat(dto.isStartBeforeEnd()).isTrue();
    }

    @Test
    void serializedBodyDoesNotContainValidationFlag() throws Exception {
        BookItemRequestDto dto = new BookItemRequestDto(30L,
                LocalDateTime.of(2030, 1, 1, 10, 0),
                LocalDateTime.of(2030, 1, 2, 10, 0));

        assertThat(json.write(dto)).doesNotHaveJsonPath("$.startBeforeEnd");
    }
}
