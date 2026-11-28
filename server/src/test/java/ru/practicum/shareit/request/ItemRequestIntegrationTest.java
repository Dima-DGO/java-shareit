package ru.practicum.shareit.request;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserRepository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ItemRequestIntegrationTest {

    @Autowired
    private ItemRequestService requestService;
    @Autowired
    private ItemRequestRepository requestRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void createAndReadRequestWithAnswer() {
        User user = userRepository.save(user("ivan@mail.com"));

        ItemRequestDto created = requestService.create(user.getId(), requestDto("Нужна дрель"));
        ItemRequest savedRequest = requestRepository.findById(created.getId()).orElseThrow();
        Item item = itemRepository.save(item(user, savedRequest));

        ItemRequestDto byId = requestService.getById(user.getId(), created.getId());
        List<ItemRequestDto> own = requestService.getOwn(user.getId());

        assertThat(byId.getItems(), hasSize(1));
        assertThat(byId.getItems().get(0).getId(), is(item.getId()));
        assertThat(byId.getItems().get(0).getName(), is("Дрель"));
        assertThat(byId.getItems().get(0).getOwnerId(), is(user.getId()));
        assertThat(own, hasSize(1));
    }

    @Test
    void getOthersExcludesOwnRequests() {
        User first = userRepository.save(user("first@mail.com"));
        User second = userRepository.save(user("second@mail.com"));

        requestService.create(first.getId(), requestDto("Запрос первого"));

        assertThat(requestService.getOthers(first.getId(), 0, 10), is(empty()));
        assertThat(requestService.getOthers(second.getId(), 0, 10), hasSize(1));
    }

    private User user(String email) {
        User user = new User();
        user.setName("Иван");
        user.setEmail(email);
        return user;
    }

    private ItemRequestDto requestDto(String description) {
        ItemRequestDto dto = new ItemRequestDto();
        dto.setDescription(description);
        return dto;
    }

    private Item item(User owner, ItemRequest request) {
        Item item = new Item();
        item.setName("Дрель");
        item.setDescription("Мощная");
        item.setAvailable(true);
        item.setOwner(owner);
        item.setRequest(request);
        return item;
    }
}
