package ru.practicum.shareit.request.dto;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ItemRequestDtoJsonTest {

    @Autowired
    private JacksonTester<ItemRequestDto> json;

    @Test
    void deserializesRequestWithEmptyAnswersList() throws Exception {
        ItemRequestDto dto = json.parseObject("{\"description\":\"Нужна дрель\"}");

        assertThat(dto.getDescription()).isEqualTo("Нужна дрель");
        assertThat(dto.getItems()).isNotNull().isEmpty();
        assertThat(dto.getCreated()).isNull();
    }

    @Test
    void serializesCreatedAtAsIsoString() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(3L);
        dto.setDescription("Нужна дрель");
        dto.setCreated(LocalDateTime.of(2030, 1, 1, 12, 0, 0));

        assertThat(json.write(dto)).hasJsonPathStringValue("$.created", "2030-01-01T12:00:00");
    }
}
