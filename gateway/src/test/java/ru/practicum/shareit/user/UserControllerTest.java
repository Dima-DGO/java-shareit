package ru.practicum.shareit.user;

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
import ru.practicum.shareit.user.dto.NewUserDto;
import ru.practicum.shareit.user.dto.UpdateUserDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest extends ControllerTestBase {

    @Mock
    private UserClient userClient;
    @InjectMocks
    private UserController controller;

    @BeforeEach
    void setUp() {
        setUpMvc(controller);
    }

    @Test
    void createUser() throws Exception {
        when(userClient.create(any(NewUserDto.class))).thenReturn(ResponseEntity.ok(Map.of("id", 5)));

        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Petr\",\"email\":\"petr@mail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(5)));

        ArgumentCaptor<NewUserDto> captor = ArgumentCaptor.forClass(NewUserDto.class);
        verify(userClient).create(captor.capture());
        assertThat(captor.getValue().getEmail(), is("petr@mail.com"));
    }

    @Test
    void createUserWithInvalidBodyReturns400() throws Exception {
        mvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        verifyNoInteractions(userClient);
    }

    @Test
    void updateUser() throws Exception {
        when(userClient.update(eq(7L), any(UpdateUserDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 7)));

        mvc.perform(patch("/users/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Petr\"}"))
                .andExpect(status().isOk());

        verify(userClient).update(eq(7L), any(UpdateUserDto.class));
    }

    @Test
    void getUser() throws Exception {
        when(userClient.get(7L)).thenReturn(ResponseEntity.ok(Map.of("id", 7)));

        mvc.perform(get("/users/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(7)));
    }

    @Test
    void deleteUser() throws Exception {
        when(userClient.delete(7L)).thenReturn(ResponseEntity.noContent().build());

        mvc.perform(delete("/users/7"))
                .andExpect(status().isNoContent());
    }
}
