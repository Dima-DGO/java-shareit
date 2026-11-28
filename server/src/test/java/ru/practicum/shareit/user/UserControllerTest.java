package ru.practicum.shareit.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void createReturnsUser() throws Exception {
        when(userService.create(any(UserDto.class))).thenReturn(user(1L, "Иван", "ivan@mail.ru"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Иван\",\"email\":\"ivan@mail.ru\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Иван"))
                .andExpect(jsonPath("$.email").value("ivan@mail.ru"));

        verify(userService).create(any(UserDto.class));
    }

    @Test
    void updatePassesIdAndBody() throws Exception {
        when(userService.update(eq(5L), any(UserDto.class))).thenReturn(user(5L, "Пётр", "petr@mail.ru"));

        mockMvc.perform(patch("/users/{userId}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Пётр\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Пётр"));

        verify(userService).update(eq(5L), any(UserDto.class));
    }

    @Test
    void getReturnsUser() throws Exception {
        when(userService.getById(5L)).thenReturn(user(5L, "Пётр", "petr@mail.ru"));

        mockMvc.perform(get("/users/{userId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("petr@mail.ru"));
    }

    @Test
    void getMissingUserReturns404WithError() throws Exception {
        when(userService.getById(99L)).thenThrow(new NotFoundException("Пользователь с id 99 не найден"));

        mockMvc.perform(get("/users/{userId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Пользователь с id 99 не найден"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/users/{userId}", 5L))
                .andExpect(status().isNoContent());

        verify(userService).delete(5L);
    }

    private UserDto user(Long id, String name, String email) {
        UserDto dto = new UserDto();
        dto.setId(id);
        dto.setName(name);
        dto.setEmail(email);
        return dto;
    }
}
