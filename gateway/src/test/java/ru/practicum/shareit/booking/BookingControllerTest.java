package ru.practicum.shareit.booking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.practicum.shareit.ControllerTestBase;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;
import ru.practicum.shareit.booking.dto.BookingState;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest extends ControllerTestBase {
    private static final String HEADER = "X-Sharer-User-Id";

    @Mock
    private BookingClient bookingClient;
    @InjectMocks
    private BookingController controller;

    @BeforeEach
    void setUp() {
        setUpMvc(controller);
    }

    @Test
    void getBookingsWithDefaults() throws Exception {
        when(bookingClient.getBookings(1L, BookingState.ALL, 0, 10))
                .thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings").header(HEADER, 1))
                .andExpect(status().isOk());

        verify(bookingClient).getBookings(1L, BookingState.ALL, 0, 10);
    }

    @Test
    void getBookingsWithStateAndPaging() throws Exception {
        when(bookingClient.getBookings(1L, BookingState.CURRENT, 10, 5))
                .thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings")
                        .header(HEADER, 1)
                        .param("state", "current")
                        .param("from", "10")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(bookingClient).getBookings(1L, BookingState.CURRENT, 10, 5);
    }

    @Test
    void unknownStateReturns400() throws Exception {
        mvc.perform(get("/bookings").header(HEADER, 1).param("state", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(bookingClient);
    }

    @Test
    void getOwnerBookings() throws Exception {
        when(bookingClient.getOwnerBookings(1L, BookingState.ALL, 0, 10))
                .thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/bookings/owner").header(HEADER, 1))
                .andExpect(status().isOk());

        verify(bookingClient).getOwnerBookings(1L, BookingState.ALL, 0, 10);
    }

    @Test
    void bookItem() throws Exception {
        when(bookingClient.bookItem(eq(2L), any(BookItemRequestDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 10)));

        String start = LocalDateTime.now().plusDays(1).withNano(0).toString();
        String end = LocalDateTime.now().plusDays(2).withNano(0).toString();

        mvc.perform(post("/bookings")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":30,\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));

        verify(bookingClient).bookItem(eq(2L), any(BookItemRequestDto.class));
    }

    @Test
    void approveBooking() throws Exception {
        when(bookingClient.approve(1L, 10L, true))
                .thenReturn(ResponseEntity.ok(Map.of("status", "APPROVED")));

        mvc.perform(patch("/bookings/10").header(HEADER, 1).param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        verify(bookingClient).approve(1L, 10L, true);
    }

    @Test
    void getBookingById() throws Exception {
        when(bookingClient.getBooking(1L, 10L))
                .thenReturn(ResponseEntity.ok(Map.of("id", 10)));

        mvc.perform(get("/bookings/10").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(10)));
    }
}
