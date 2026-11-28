package ru.practicum.shareit.booking;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingRequestDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookingService bookingService;

    @Test
    void createReturnsBooking() throws Exception {
        BookingDto dto = new BookingDto();
        dto.setId(1L);
        dto.setStatus(BookingStatus.WAITING);
        when(bookingService.create(eq(2L), any(BookingRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post("/bookings")
                        .header(USER_HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":5,\"start\":\"2030-01-01T10:00:00\","
                                + "\"end\":\"2030-01-05T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("WAITING"));

        verify(bookingService).create(eq(2L), any(BookingRequestDto.class));
    }

    @Test
    void approvePassesFlag() throws Exception {
        BookingDto dto = new BookingDto();
        dto.setStatus(BookingStatus.APPROVED);
        when(bookingService.approve(1L, 1L, true)).thenReturn(dto);

        mockMvc.perform(patch("/bookings/{bookingId}", 1L)
                        .header(USER_HEADER, 1)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        verify(bookingService).approve(1L, 1L, true);
    }

    @Test
    void getByIdReturnsBooking() throws Exception {
        BookingDto dto = new BookingDto();
        dto.setId(4L);
        when(bookingService.getById(2L, 4L)).thenReturn(dto);

        mockMvc.perform(get("/bookings/{bookingId}", 4L).header(USER_HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void getByBookerPassesState() throws Exception {
        when(bookingService.getByBooker(2L, BookingState.WAITING)).thenReturn(List.of());

        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, 2)
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(bookingService).getByBooker(2L, BookingState.WAITING);
    }

    @Test
    void getByOwnerUsesAllByDefault() throws Exception {
        when(bookingService.getByOwner(2L, BookingState.ALL)).thenReturn(List.of());

        mockMvc.perform(get("/bookings/owner").header(USER_HEADER, 2))
                .andExpect(status().isOk());

        verify(bookingService).getByOwner(2L, BookingState.ALL);
    }

    @Test
    void unknownStateReturns400() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header(USER_HEADER, 2)
                        .param("state", "WRONG"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
