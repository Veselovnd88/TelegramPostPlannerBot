package ru.veselov.plannerBot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.cache.AdminActionsDataCache;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
@Disabled
public class StatusChangingTest {

    @Autowired
    DataCache userDataCache;
    @Autowired
    AdminActionsDataCache adminActionsDataCache;
    @MockBean
    UserService userService;
    @MockBean
    PostService postService;
    @Autowired
    UpdateController updateController;
    UserActions actions = new UserActions();
    User user = new User();
    List mockList;
    @BeforeEach
    public void init(){
        user.setId(-105L);
        user.setUserName("Vasya");
        user.setFirstName("Zloy");
        user.setLastName("Evil");
        mockList = Mockito.mock(ArrayList.class);
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
        updateController.processUpdate(actions.userCreatePost(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void createPostWhenAnotherState(){
        /*Нажатие кнопки, когда статус бота НЕ READY TO WORK*/
        for (BotState s : BotState.values()){
            if(s!=BotState.READY_TO_WORK){
                userDataCache.setUserBotState(user.getId(),s);
                updateController.processUpdate(actions.userCreatePost(user));
                assertEquals(s,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }

    @Test
    void pressCreatePostWhenUserHasChannelsLimitIsOk(){
        /*Нажатие кнопки при подключенных каналах*/
        userDataCache.setUserBotState(user.getId(),BotState.READY_TO_WORK);
        //Проверка на лимит постов//
        when(postService.findByUserAndPostStates(user,List.of(PostState.SAVED,PostState.PLANNED))).thenReturn(mockList);
        when(mockList.size()).thenReturn(5);
        when(userService.getUserMaxPosts(user)).thenReturn(6);
        updateController.processUpdate(actions.userCreatePost(user));
        assertEquals(BotState.AWAITING_POST, userDataCache.getUsersBotState(user.getId()));
    }

    @Test
    void pressCreatePostWhenUserHasChannelUnlimited(){
        //Безлимитные посты
        userDataCache.setUserBotState(user.getId(),BotState.READY_TO_WORK);
        when(mockList.size()).thenReturn(5);
        when(postService.findByUserAndPostStates(user,List.of(PostState.SAVED,PostState.PLANNED))).thenReturn(mockList);
        when(userService.getUserMaxPosts(user)).thenReturn(-1);
        updateController.processUpdate(actions.userCreatePost(user));
        assertEquals(BotState.AWAITING_POST, userDataCache.getUsersBotState(user.getId()));
    }

    @Test
    void pressCreatePostWhenUserHasChannelUnderLimit(){
        //Лимит превышения
        userDataCache.setUserBotState(user.getId(),BotState.READY_TO_WORK);
        when(mockList.size()).thenReturn(5);
        when(postService.findByUserAndPostStates(user,List.of(PostState.SAVED,PostState.PLANNED))).thenReturn(mockList);
        when(userService.getUserMaxPosts(user)).thenReturn(4);
        updateController.processUpdate(actions.userCreatePost(user));
        assertEquals(BotState.READY_TO_WORK, userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void userSentContentText(){
        //Проверка на то, что апдейт попадает в этот метод, только при статусе ожидания поста
        userDataCache.setUserBotState(user.getId(),BotState.AWAITING_POST);
        userDataCache.createPostCreator(user);
        updateController.processUpdate(actions.userSendText(user));
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
    }

    @Test
    void userSentContentTextAnotherBotStates(){
        //Проверка на то, что в из других состояний метод не изменит статус бота
        for (BotState s : BotState.values()){
            if(s!=BotState.AWAITING_POST){
                userDataCache.setUserBotState(user.getId(),s);
                userDataCache.createPostCreator(user);
                updateController.processUpdate(actions.userSendText(user));
                assertNotEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }
    @Test
    void userPressSavePostButton(){
        //Проверка изменения статуса при нажатии кнопки сохранить пост в канал
        userDataCache.setUserBotState(user.getId(),BotState.AWAITING_POST);
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        Chat chat = new Chat();
        chat.setId(2L);
        String chatName = "test1";
        chat.setTitle(chatName);
        when(userService.findAllChatsByUser(user)).thenReturn(Set.of(chat));
        updateController.processUpdate(actions.userPressButtonForChoseChanel(user,chatName));
        assertEquals(BotState.AWAITING_DATE, userDataCache.getUsersBotState(user.getId()));
        //Когда нажали Запостить во все чаты
        updateController.processUpdate(actions.userPressButtonForChoseChanel(user,"postAll"));
        assertEquals(BotState.AWAITING_DATE, userDataCache.getUsersBotState(user.getId()));
    }

    @Test
    void pressSaveAnotherStates(){
        //Проверка на то, что из другого состояния метод не изменит состояние бота на ожидание даты
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        for(BotState s: BotState.values()){
            if(s!=BotState.AWAITING_POST){
                updateController.processUpdate(actions.userPressButtonForChoseChanel(user,"postAll"));
                assertNotEquals(BotState.AWAITING_DATE,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }
    @Test
    void userChooseDate(){
        userDataCache.setUserBotState(user.getId(),BotState.AWAITING_DATE);
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        updateController.processUpdate(actions.userInputDate(user,"31.12.2022 15 00"));
        assertEquals(BotState.READY_TO_SAVE,userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void userChooseDateWrongState(){
        //Проверка на то, что метод переведет бота в статус READY_TO SAVE только из статуса ожидания сохранения
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        for(BotState s: BotState.values()){
            if(s!=BotState.AWAITING_DATE){
                updateController.processUpdate(actions.userInputDate(user,"31.12.2022 15 00"));
                assertNotEquals(BotState.READY_TO_SAVE,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }

    @Test
    void userSaveDate(){
        //Проверка изменения статуса после сохранения даты, переход в базовое состояние
        userDataCache.setUserBotState(user.getId(),BotState.READY_TO_SAVE);
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        updateController.processUpdate(actions.userSavedDate(user));
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void userSaveDateWrongState(){
        //Проверка на то, что метод переведет бота в базовый статус только из состояния READY_TO_SAVE
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        for(BotState s: BotState.values()){
            if(s!=BotState.READY_TO_SAVE){
                updateController.processUpdate(actions.userSavedDate(user));
                assertNotEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }

    @Test
    void userPressAgainInputDate(){
        //Проверка того, что статус меняется на ожидание даты при нажатии пользователем кнопки о выборе другой даты
        userDataCache.setUserBotState(user.getId(),BotState.READY_TO_SAVE);
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        updateController.processUpdate(actions.userInputDateAgain(user));
        assertEquals(BotState.AWAITING_DATE,userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void userPressAgainInputDateWrongStateTest(){
        //Проверка на то, что метод меняет статус только из состояния Ready to save
        userDataCache.createPostCreator(user);
        userDataCache.getPostCreator(user.getId()).addText("Text");
        for(BotState s: BotState.values()){
            if(s!=BotState.READY_TO_SAVE){
                updateController.processUpdate(actions.userInputDateAgain(user));
                assertNotEquals(BotState.AWAITING_DATE,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }

    @Test
    void resetTest(){
        //Проверка нажатия /reset
        userDataCache.setUserBotState(user.getId(),BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        updateController.processUpdate(actions.userReset(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
        for(BotState s: BotState.values()){
            if(s!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                userDataCache.setUserBotState(user.getId(),s);
                updateController.processUpdate(actions.userReset(user));
                assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }

    @Test
    void helpTest(){
        //Проверка нажатия /help, т.к. внутри срабатывает reset - поведение аналогичное reset
        userDataCache.setUserBotState(user.getId(),BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        updateController.processUpdate(actions.userPressHelp(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
        for(BotState s: BotState.values()){
            if(s!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                userDataCache.setUserBotState(user.getId(),s);
                updateController.processUpdate(actions.userReset(user));
                assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }
    @Test
    void viewTest(){
        //Проверка изменения состояния при нажатии кнопки /view
        userDataCache.setUserBotState(user.getId(),BotState.READY_TO_WORK);
        updateController.processUpdate(actions.userPressView(user));
        assertEquals(BotState.VIEW,userDataCache.getUsersBotState(user.getId()));
    }
    @Test
    void viewTestWrongStatus(){
        //Проверка на то, что метод отрабатывает только из состояния READY TO WORK
        for(BotState s: BotState.values()){
            if(s!=BotState.READY_TO_WORK){
                userDataCache.setUserBotState(user.getId(),s);
                updateController.processUpdate(actions.userPressView(user));
                assertEquals(s,userDataCache.getUsersBotState(user.getId()));
            }
        }
    }
    @Test
    void promoteTest(){
        //Проверка, что при нажатии команды состояние меняется из любого состояния
        for(BotState s: BotState.values()){
                userDataCache.setUserBotState(user.getId(),s);
                updateController.processUpdate(actions.adminPressPromote(user));
                assertEquals(BotState.PROMOTE_USER,userDataCache.getUsersBotState(user.getId()));
            }
    }
    @Test
    void adminPromoteUserTest(){
        //Проверка на то, что состояние меняется на первоначальное после действий
        userDataCache.setUserBotState(user.getId(),BotState.PROMOTE_USER);
        adminActionsDataCache.setStartBotState(user.getId(),BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        adminActionsDataCache.setPromoteUser(user.getId(),new User());
        when(userService.findUserById(user)).thenReturn(Optional.of(new UserEntity()));
        updateController.processUpdate(actions.adminPromoteUser(user));
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
        //////проверка другого состояния из которого может вызвать
        userDataCache.setUserBotState(user.getId(),BotState.PROMOTE_USER);
        adminActionsDataCache.setStartBotState(user.getId(),BotState.READY_TO_WORK);
        adminActionsDataCache.setPromoteUser(user.getId(),new User());
        when(userService.findUserById(user)).thenReturn(Optional.of(new UserEntity()));
        updateController.processUpdate(actions.adminPromoteUser(user));
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
    }

    //TODO проверка MANAGE


}
