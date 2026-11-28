package ru.practicum.shareit.request;

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
import ru.practicum.shareit.request.dto.NewItemRequestDto;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest extends ControllerTestBase {
    private static final String HEADER = "X-Sharer-User-Id";

    @Mock
    private RequestClient requestClient;
    @InjectMocks
    private RequestController controller;

    @BeforeEach
    void setUp() {
        setUpMvc(controller);
    }

    @Test
    void createRequest() throws Exception {
        when(requestClient.create(eq(2L), any(NewItemRequestDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 3)));

        mvc.perform(post("/requests")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Need a ladder\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));

        verify(requestClient).create(eq(2L), any(NewItemRequestDto.class));
    }

    @Test
    void getOwnRequests() throws Exception {
        when(requestClient.getOwn(2L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 3))));

        mvc.perform(get("/requests").header(HEADER, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(3)));
    }

    @Test
    void getOtherUsersRequests() throws Exception {
        when(requestClient.getOthers(2L)).thenReturn(ResponseEntity.ok(List.of()));

        mvc.perform(get("/requests/all").header(HEADER, 2))
                .andExpect(status().isOk());

        verify(requestClient).getOthers(2L);
    }

    @Test
    void getRequestById() throws Exception {
        when(requestClient.getById(1L, 3L)).thenReturn(ResponseEntity.ok(Map.of("id", 3)));

        mvc.perform(get("/requests/3").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(3)));
    }
}
