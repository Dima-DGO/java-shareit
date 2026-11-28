package ru.practicum.shareit.request;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.practicum.shareit.request.dto.ItemRequestDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemRequestController.class)
class ItemRequestControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    void createReturnsRequest() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(3L);
        dto.setDescription("Нужна дрель");
        when(itemRequestService.create(eq(1L), any(ItemRequestDto.class))).thenReturn(dto);

        mockMvc.perform(post("/requests")
                        .header(USER_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Нужна дрель\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.description").value("Нужна дрель"))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void getOwnReturnsList() throws Exception {
        when(itemRequestService.getOwn(1L)).thenReturn(List.of(new ItemRequestDto()));

        mockMvc.perform(get("/requests").header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getOthersReturnsAllOtherRequests() throws Exception {
        when(itemRequestService.getOthers(1L)).thenReturn(List.of());

        mockMvc.perform(get("/requests/all").header(USER_HEADER, 1))
                .andExpect(status().isOk());

        verify(itemRequestService).getOthers(1L);
    }

    @Test
    void getByIdReturnsRequest() throws Exception {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setId(4L);
        when(itemRequestService.getById(1L, 4L)).thenReturn(dto);

        mockMvc.perform(get("/requests/{requestId}", 4L).header(USER_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4));
    }
}
