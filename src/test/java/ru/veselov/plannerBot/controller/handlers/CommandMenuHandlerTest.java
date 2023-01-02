package ru.veselov.plannerBot.controller.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.SchedulingService;
import ru.veselov.plannerBot.service.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static ru.veselov.plannerBot.utils.MessageUtils.*;

@SpringBootTest
@Disabled
class CommandMenuHandlerTest {
    @MockBean
    private MyPreciousBot bot;

    @MockBean
    private UserService userService;
    @MockBean
    private PostService postService;
    @MockBean
    private DataCache userDataCache;
    @MockBean
    private SchedulingService schedulingService;
    @Autowired
    private CommandMenuHandler commandMenuHandler;

    private final Update mockUpdate = Mockito.mock(Update.class);
    private final Chat mockChat = Mockito.mock(Chat.class);
    private final Message mockMessage = Mockito.mock(Message.class);
    private final User mockUser = Mockito.mock(User.class);

    @BeforeEach
    void init(){
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getFrom()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);
        when(mockMessage.getChatId()).thenReturn(2L);
    }

    @Test
    void testStartCommand(){
        when(mockMessage.getText()).thenReturn("/start");
        when(userDataCache.getUsersBotState(1L)).thenReturn(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        Chat chat = new Chat();
        chat.setId(2L);
        chat.setTitle("test1");
        when(userService.findAllChatsByUser(mockUser)).thenReturn(Set.of(chat));
        assertEquals(START_MESSAGE_USER_ALREADY_USE_BOT+Set.of(chat.getTitle()),commandMenuHandler.processUpdate(mockUpdate).getText());
        when(userService.findAllChatsByUser(mockUser)).thenReturn(Collections.emptySet());
        assertEquals(START_MESSAGE,commandMenuHandler.processUpdate(mockUpdate).getText());
        when(userDataCache.getUsersBotState(1L)).thenReturn(BotState.AWAITING_POST);
        when(userService.findAllChatsByUser(mockUser)).thenReturn(Set.of(chat));
        assertEquals(START_MESSAGE_USER_ALREADY_USE_BOT+Set.of(chat.getTitle()),commandMenuHandler.processUpdate(mockUpdate).getText());
        when(userDataCache.getUsersBotState(1L)).thenReturn(BotState.AWAITING_POST);
        when(userService.findAllChatsByUser(mockUser)).thenReturn(Collections.emptySet());
        assertEquals(START_MESSAGE,commandMenuHandler.processUpdate(mockUpdate).getText());
    }

    /*Проверка прохода по команде /create*/
    @Test
    void testCreateCommand(){
        when(mockMessage.getText()).thenReturn("/create");
        /*Количество постов не превышает максимального*/
        when(postService.findByUserAndPostStates(mockUser, List.of(PostState.SAVED,PostState.PLANNED)))
                .thenReturn(List.of(new Post(), new Post()));
        when(userService.getUserMaxPosts(mockUser)).thenReturn(10);
        when(userDataCache.getUsersBotState(1L)).thenReturn(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        //если бот никуда не добавлен
        assertEquals(BOT_WAS_NOT_ADDED_TO_CHANEL,
                commandMenuHandler.processUpdate(mockUpdate).getText());
        int max = 1;
        when(userService.getUserMaxPosts(mockUser)).thenReturn(max);
        assertEquals(POST_LIMIT+max,commandMenuHandler.processUpdate(mockUpdate).getText());
        when(userDataCache.getUsersBotState(1L)).thenReturn(BotState.AWAITING_POST);
        //Если бот добавлен в чаты
        when(userService.getUserMaxPosts(mockUser)).thenReturn(-1);
        assertEquals(AWAIT_CONTENT_MESSAGE,commandMenuHandler.processUpdate(mockUpdate).getText());
        //количество вызовов сервиса для установки флага в редактирования поста в тру
        verify(userDataCache,times(1)).createPost(mockUser);
    }

    @Test
    void testViewCommand(){
        when(mockMessage.getText()).thenReturn("/view");
        when(postService.findByUserAndPostStates(mockUser,List.of(PostState.SAVED,PostState.PLANNED)))
                .thenReturn(Collections.emptyList());
        assertEquals(NO_PLANNED_POSTS,commandMenuHandler.processUpdate(mockUpdate).getText());
        when(postService.findByUserAndPostStates(mockUser,List.of(PostState.SAVED,PostState.PLANNED)))
                .thenReturn(List.of(new Post(), new Post()));
        assertTrue(commandMenuHandler.processUpdate(mockUpdate).getText().startsWith("Запланированные"));
    }

    @Test
    void testResetCommand(){
        when(mockMessage.getText()).thenReturn("/reset");
        assertEquals(RESET_POSTS,commandMenuHandler.processUpdate(mockUpdate).getText());
        verify(userDataCache, times(1)).clear(1L);

    }

    @Test
    void testHelpCommand(){
        when(mockMessage.getText()).thenReturn("/help");
        assertEquals(HELP_MESSAGE,commandMenuHandler.processUpdate(mockUpdate).getText());
    }




}