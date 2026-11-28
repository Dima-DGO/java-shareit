package ru.practicum.shareit.item;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.practicum.shareit.ControllerTestBase;
import ru.practicum.shareit.item.dto.NewCommentDto;
import ru.practicum.shareit.item.dto.NewItemDto;
import ru.practicum.shareit.item.dto.UpdateItemDto;

import static org.hamcrest.MatcherAssert.assertThat;
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
class ItemControllerTest extends ControllerTestBase {
    private static final String HEADER = "X-Sharer-User-Id";

    @Mock
    private ItemClient itemClient;
    @InjectMocks
    private ItemController controller;

    @BeforeEach
    void setUp() {
        setUpMvc(controller);
    }

    @Test
    void createItem() throws Exception {
        when(itemClient.create(eq(1L), any(NewItemDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 30)));

        mvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Drill\",\"description\":\"Powerful\",\"available\":true,\"requestId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(30)));

        ArgumentCaptor<NewItemDto> captor = ArgumentCaptor.forClass(NewItemDto.class);
        verify(itemClient).create(eq(1L), captor.capture());
        assertThat(captor.getValue().getRequestId(), is(2L));
    }

    @Test
    void createItemWithoutNameReturns400() throws Exception {
        mvc.perform(post("/items")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Powerful\",\"available\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(itemClient);
    }

    @Test
    void updateItem() throws Exception {
        when(itemClient.update(eq(1L), eq(30L), any(UpdateItemDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 30)));

        mvc.perform(patch("/items/30")
                        .header(HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk());

        verify(itemClient).update(eq(1L), eq(30L), any(UpdateItemDto.class));
    }

    @Test
    void getItem() throws Exception {
        when(itemClient.get(1L, 30L)).thenReturn(ResponseEntity.ok(Map.of("id", 30)));

        mvc.perform(get("/items/30").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(30)));
    }

    @Test
    void getOwnerItems() throws Exception {
        when(itemClient.getByOwner(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 30))));

        mvc.perform(get("/items").header(HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(30)));
    }

    @Test
    void searchItems() throws Exception {
        when(itemClient.search("Drill")).thenReturn(ResponseEntity.ok(List.of(Map.of("name", "Drill"))));

        mvc.perform(get("/items/search").param("text", "Drill"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name", is("Drill")));

        verify(itemClient).search("Drill");
    }

    @Test
    void addComment() throws Exception {
        when(itemClient.addComment(eq(2L), eq(30L), any(NewCommentDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 1)));

        mvc.perform(post("/items/30/comment")
                        .header(HEADER, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Good\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<NewCommentDto> captor = ArgumentCaptor.forClass(NewCommentDto.class);
        verify(itemClient).addComment(eq(2L), eq(30L), captor.capture());
        assertThat(captor.getValue().getText(), is("Good"));
    }
}
