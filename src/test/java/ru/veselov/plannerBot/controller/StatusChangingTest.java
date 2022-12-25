package ru.veselov.plannerBot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.BotProperties;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/*TODO перенести тест на по смене статусов сюда*/
@SpringBootTest
@Disabled
public class StatusChangingTest {

    @MockBean
    MyPreciousBot bot;
    @Autowired
    DataCache userDataCache;
    @MockBean
    UserService userService;
    @Autowired
    private BotProperties botProperties;
    @MockBean
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
        /*Если бота убрали из канала, то бот уходит в статус ожидания канала*/
        userDataCache.setUserBotState(user.getId(),BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        //
        }

    @Test
    void userFirstContact(){
        /*Первый контакт пользователя с ботом, переводит бота в состояние ожидания канала,
        * то же самое если бот находится в состоянии ожидания канала*/
        updateController.processUpdate(actions.userPressStart(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void userAlreadyHadChats(){
        /*У пользователя уже есть каналы, сохраненный в БД, но нет кеша, после перезапуска бота
        * пользователь жмет старт*/
        Chat chat = new Chat();
        chat.setId(2L);
        chat.setTitle("test1");
        when(userService.findAllChatsByUser(user)).thenReturn(Set.of(chat));
        updateController.processUpdate(actions.userPressStart(user));
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
    }

    @Test
    void pressStartFromAnotherStates(){
        /*При нажатии старт бот переводится в состояние READY_TO_WORK, кроме случая когда бот не добавлен в канал */
        Chat chat = new Chat();
        chat.setId(2L);
        chat.setTitle("test1");
        when(userService.findAllChatsByUser(user)).thenReturn(Set.of(chat));
        for(BotState s: BotState.values()){
            userDataCache.setUserBotState(user.getId(),s);
            updateController.processUpdate(actions.userPressStart(user));
            assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        }
    }
    @Test
    void pressCreatePostNoChannels(){
        /*Нажатие кнопки при отсутствии подключенных каналов*/
        updateController.processUpdate(actions.userPressStart(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
    }

    @Test
    void pressCreatePostWhenUserHasChannels(){
        /*Нажатие кнопки при подключенных каналах
        * TODO проверка смены статуса на AWAITING POST при всех статусах бота, кроме ожидания*/
    }

    //TODO проверка смены статуса с AWAITING POST на AWAITING DATE
    //TODO проверка смены статуса c AWAITING DATE на READY TO SAVE
    //TODO проверка смены статуса с READY TO SAVE на READY TO WORK

    //TODO проверка сброса статуса командой reset
    //TODO проверка help из любого статуса
    //TODO проверка promote из любого статуса
    //TODO проверка VIEW
    //TODO проверка MANAGE


}
