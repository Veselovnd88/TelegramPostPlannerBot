package ru.veselov.plannerBot.controller;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.handlers.AddDataHandler;
import ru.veselov.plannerBot.controller.handlers.CallBackQueriesHandler;
import ru.veselov.plannerBot.controller.handlers.CreatePostHandler;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.service.SchedulingService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.BotProperties;

import static org.mockito.Mockito.*;


@SpringBootTest
class UpdateControllerFunctionTest {


    private final Long botId = 5655105778L;

    @Autowired
    private MyPreciousBot bot;

    @Autowired
    private UserService userService;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private BotProperties botProperties;
    @Autowired
    TelegramBotsApi telegramBotsApi;
    @MockBean
    private  CreatePostHandler createPostHandler;
    @MockBean
    private AddDataHandler addDataHandler;
    @MockBean
    private CallBackQueriesHandler callBackQueriesHandler;
    @MockBean
    private SchedulingService service;
    @MockBean
    private DataCache userDataCache;
    private Update mockUpdate;
    private ChatMemberUpdated myChatMember;
    private ChatMember mockChatMember;
    private User mockUser;
    private Chat mockChat;
    private Message mockMessage;
    @Autowired
    private UpdateController updateController;

    @BeforeEach
    public void init(){
        mockUpdate = Mockito.mock(Update.class);
        myChatMember = Mockito.mock(ChatMemberUpdated.class);
        mockChatMember = Mockito.mock(ChatMember.class);
        mockUser = Mockito.mock(User.class);
        mockChat = Mockito.mock(Chat.class);
        mockMessage = Mockito.mock(Message.class);
    }

    /*Проверка на случай когда при подключении бота к нему не приходит Id канала*/
    @Test
    void processUpdateWhenAddingBotNotBotId() throws TelegramApiException {
        when(mockUpdate.hasMyChatMember()).thenReturn(true);
        when(mockUpdate.getMyChatMember()).thenReturn(myChatMember);
        when(myChatMember.getNewChatMember()).thenReturn(mockChatMember);
        when(mockChatMember.getUser()).thenReturn(mockUser);
        when(mockChatMember.getStatus()).thenReturn("Unknown status");
        when(mockUser.getId()).thenReturn(1L);
        updateController.processUpdate(mockUpdate);
    }
    /*Проверка сохранения, удаления канала, когда приходит update с присоединением/отсоединением бота*/
    @Test
    void processUpdateWhenAddingBotBotId(){
        when(mockUpdate.hasMyChatMember()).thenReturn(true);
        when(mockUpdate.getMyChatMember()).thenReturn(myChatMember);
        when(myChatMember.getNewChatMember()).thenReturn(mockChatMember);
        when(mockChatMember.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(botId);
        when(myChatMember.getChat()).thenReturn(mockChat);
        when(mockChat.getTitle()).thenReturn("Mock Chat Title");
        when(mockChat.getId()).thenReturn(111L);
        when(mockChatMember.getStatus()).thenReturn("administrator");
        when(myChatMember.getFrom()).thenReturn(mockUser);
        updateController.processUpdate(mockUpdate);
        Assertions.assertEquals(1, userService.findAllChatsByUser(mockUser).size());
        when(mockChatMember.getStatus()).thenReturn("left");
        updateController.processUpdate(mockUpdate);
        Assertions.assertEquals(0, userService.findAllChatsByUser(mockUser).size());
        when(mockChatMember.getStatus()).thenReturn("kicked");
        updateController.processUpdate(mockUpdate);
        Assertions.assertEquals(0, userService.findAllChatsByUser(mockUser).size());
        when(mockChatMember.getStatus()).thenReturn("");
        updateController.processUpdate(mockUpdate);
        Assertions.assertEquals(0, userService.findAllChatsByUser(mockUser).size());
        when(mockChatMember.getStatus()).thenReturn("asdf");
        updateController.processUpdate(mockUpdate);
        Assertions.assertEquals(0, userService.findAllChatsByUser(mockUser).size());
        userService.removeUser(mockUser);
        Assertions.assertFalse(userService.findUserById(mockUser).isPresent());
    }
    /*Проверка когда при подключении бота выпадает ошибка*/
    @Test
    void processUpdateWhenAddingBotThrowException(){
        when(mockUpdate.hasMyChatMember()).thenReturn(true);
        doAnswer(x->{
            throw new TelegramApiException("Mock Exception");
        }).when(mockUpdate).getMyChatMember();
        updateController.processUpdate(mockUpdate);
    }
}