package ru.practicum.shareit.item;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemResponseDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    private static final String USER_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Test
    void createPassesUserIdAndBody() throws Exception {
        ItemDto dto = new ItemDto();
        dto.setId(1L);
        dto.setName("Дрель");
        dto.setAvailable(true);
        dto.setRequestId(3L);
        when(itemService.create(eq(7L), any(ItemDto.class))).thenReturn(dto);

        mockMvc.perform(post("/items")
                        .header(USER_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Дрель\",\"description\":\"Новая\",\"available\":true,"
                                + "\"requestId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.requestId").value(3));

        verify(itemService).create(eq(7L), any(ItemDto.class));
    }

    @Test
    void updatePassesItemId() throws Exception {
        ItemDto dto = new ItemDto();
        dto.setId(1L);
        dto.setAvailable(false);
        when(itemService.update(eq(7L), eq(1L), any(ItemDto.class))).thenReturn(dto);

        mockMvc.perform(patch("/items/{itemId}", 1L)
                        .header(USER_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void getReturnsItemWithRequestIdAndComments() throws Exception {
        ItemResponseDto dto = new ItemResponseDto();
        dto.setId(1L);
        dto.setName("Дрель");
        dto.setAvailable(true);
        dto.setRequestId(3L);
        dto.setComments(List.of());
        when(itemService.getById(7L, 1L)).thenReturn(dto);

        mockMvc.perform(get("/items/{itemId}", 1L).header(USER_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(3))
                .andExpect(jsonPath("$.comments.length()").value(0));
    }

    @Test
    void getByOwnerReturnsList() throws Exception {
        when(itemService.getByOwner(7L)).thenReturn(List.of(new ItemResponseDto()));

        mockMvc.perform(get("/items").header(USER_HEADER, 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void searchPassesText() throws Exception {
        when(itemService.search("дрель")).thenReturn(List.of(new ItemResponseDto()));

        mockMvc.perform(get("/items/search").param("text", "дрель"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(itemService).search("дрель");
    }

    @Test
    void searchWithoutTextReturns400() throws Exception {
        mockMvc.perform(get("/items/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addCommentReturnsComment() throws Exception {
        CommentDto dto = new CommentDto();
        dto.setId(9L);
        dto.setText("Отличная дрель");
        dto.setAuthorName("Пётр");
        when(itemService.addComment(eq(7L), eq(1L), any(CommentDto.class))).thenReturn(dto);

        mockMvc.perform(post("/items/{itemId}/comment", 1L)
                        .header(USER_HEADER, 7)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Отличная дрель\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.authorName").value("Пётр"));
    }
}
