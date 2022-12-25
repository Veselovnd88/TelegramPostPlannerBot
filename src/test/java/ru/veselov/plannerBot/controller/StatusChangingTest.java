package ru.veselov.plannerBot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.BotProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;

/*TODO перенести тест на по смене статусов сюда*/
@SpringBootTest
@Disabled
public class StatusChangingTest {

    @Autowired
    MyPreciousBot bot;
    @Autowired
    DataCache userDataCache;
    @MockBean
    UserService userService;
    @Autowired
    private BotProperties botProperties;
    @Autowired
    TelegramBotsApi telegramBotsApi;
    @Autowired
    UpdateController updateController;
    UserActions actions = new UserActions();
    User user = new User();
    @BeforeEach
    public void init(){
        user.setId(-105L);
        user.setUserName("Vasya");
        user.setFirstName("Zloy");
        user.setLastName("Evil");

        //
        }

    @Test
    void userFirstContact(){
        updateController.processUpdate(actions.userPressStart(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
    }

    void userAlreadyHadChats(){

    }

}
