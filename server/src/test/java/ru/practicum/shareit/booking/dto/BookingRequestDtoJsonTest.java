package ru.practicum.shareit.booking.dto;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingRequestDtoJsonTest {

    @Autowired
    private JacksonTester<BookingRequestDto> json;

    @Test
    void deserializesIsoDates() throws Exception {
        BookingRequestDto dto = json.parseObject(
                "{\"itemId\":5,\"start\":\"2030-01-01T10:15:30\",\"end\":\"2030-01-05T18:00:00\"}");

        assertThat(dto.getItemId()).isEqualTo(5L);
        assertThat(dto.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 1, 10, 15, 30));
        assertThat(dto.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 5, 18, 0, 0));
    }

    @Test
    void serializesDatesWithoutTimezone() throws Exception {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setItemId(5L);
        dto.setStart(LocalDateTime.of(2030, 1, 1, 10, 15, 30));
        dto.setEnd(LocalDateTime.of(2030, 1, 5, 18, 0, 0));

        assertThat(json.write(dto)).hasJsonPathStringValue("$.start", "2030-01-01T10:15:30");
        assertThat(json.write(dto)).hasJsonPathStringValue("$.end", "2030-01-05T18:00:00");
    }
}
