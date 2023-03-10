package ru.veselov.plannerBot.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.model.Post;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class SchedulingServiceTest {
    @Autowired
    private DataCache dataCache;
    @Autowired
    private SchedulingService service;


    @Test
    void checkPostCreators() {
        User user = new User();
        user.setId(1111L);
        User user2 = new User();
        user2.setId(2222L);
        dataCache.createPost(user);
        dataCache.createPost(user2);
        assertEquals(2,dataCache.getPostCache().size());
        dataCache.setUserBotState(user.getId(),BotState.AWAITING_POST);
        dataCache.setUserBotState(user2.getId(),BotState.AWAITING_POST);
        Post post = dataCache.getPost(user.getId());
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");
        try {
            post.setDate(sdf.parse("03.12.2022 15 20"));
        } catch (ParseException ignored) {

        }
        service.checkPostCreators();
        Assertions.assertEquals(1,dataCache.getPostCache().size());
        assertEquals(BotState.READY_TO_WORK,dataCache.getUsersBotState(user.getId()));
        assertEquals(BotState.AWAITING_POST,dataCache.getUsersBotState(user2.getId()));
    }
}