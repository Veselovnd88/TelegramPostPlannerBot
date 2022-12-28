package ru.veselov.plannerBot.controller.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.controller.UpdateController;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.service.PostCreator;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.SchedulingService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.BotProperties;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest
class CallBackQueriesHandlerTest {

    @Autowired
    private MyPreciousBot bot;

    @Autowired
    private BotProperties botProperties;
    @Autowired
    TelegramBotsApi telegramBotsApi;
    @MockBean
    UserService userService;
    @MockBean
    PostService postService;
    @MockBean
    private DataCache userDataCache;
    @MockBean
    private SchedulingService service;
    private Update mockUpdate;

    private User mockUser;

    private CallbackQuery mockCallbackQuery;
    PostCreator mockCreator = Mockito.mock(PostCreator.class);
    @Autowired
    private UpdateController updateController;

    @Autowired
    private CallBackQueriesHandler callBackQueriesHandler;


    @BeforeEach
    public void init(){
        mockUpdate = Mockito.mock(Update.class);
        mockUser = Mockito.mock(User.class);
        mockCallbackQuery=Mockito.mock(CallbackQuery.class);

    }


    /*Проверка коллбэков когда нет совпадений на кнопках*/
    @Test
    void processUpdateCallBackQueriesUnknownDataFromButton(){
        //Проверяется как отрабатываются названия чатов на кнопках
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallbackQuery);
        when(mockCallbackQuery.getFrom()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);
        setUpEnteringWhenContainsPostCreator(BotState.AWAITING_POST);
        when(mockCallbackQuery.getData()).thenReturn("UNKNOWN Data");
        when(mockCallbackQuery.getId()).thenReturn("asdf");
        assertEquals(MessageUtils.INLINE_BUTTON_WITH_UNKNOWN_DATA,
                ((AnswerCallbackQuery)callBackQueriesHandler.processUpdate(mockUpdate)).getText());

    }
    /*Проверка приходящих с кнопок коллбэков*/
    @Test
    void processUpdateCallBackQueriesChosenChat(){

        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallbackQuery);
        Message mockMessage = Mockito.mock(Message.class);
        when(mockCallbackQuery.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(1L);
        when(mockCallbackQuery.getFrom()).thenReturn(mockUser);
        //через состояния бота
        setUpEnteringWhenContainsPostCreator(BotState.AWAITING_POST);
        when(mockUser.getId()).thenReturn(1L);
        Chat chat1 = Mockito.mock(Chat.class);
        Chat chat2 = Mockito.mock(Chat.class);
        Chat chat3 = Mockito.mock(Chat.class);
        when(chat1.getTitle()).thenReturn("Good Title");
        when(chat2.getTitle()).thenReturn("Очень длинное название......................................." +
                "фвыафываааааааааааааааааааааааааааааааааааааааааааааа");
        when(chat3.getTitle()).thenReturn("");

        when(mockCallbackQuery.getData()).thenReturn(MessageUtils.shortenString("Good Title"));
        when(mockCallbackQuery.getId()).thenReturn("string");
        when(userDataCache.getPostCreator(anyLong())).thenReturn(mockCreator);
        when(userService.findAllChatsByUser(mockUser)).thenReturn(Set.of(chat1,chat2,chat3));

        assertEquals(MessageUtils.AWAITING_DATE,
                ((SendMessage) callBackQueriesHandler.processUpdate(mockUpdate)).getText());

        when(mockCallbackQuery.getData()).thenReturn(
                MessageUtils.shortenString("Очень длинное название......................................" +
                        ".фвыафываааааааааааааааааааааааааааааааааааааааааааааа"));
        assertEquals(MessageUtils.AWAITING_DATE,
                ((SendMessage)callBackQueriesHandler.processUpdate(mockUpdate)).getText());

        when(mockCallbackQuery.getData()).thenReturn(MessageUtils.shortenString(""));
        assertEquals(MessageUtils.AWAITING_DATE,
                ((SendMessage) callBackQueriesHandler.processUpdate(mockUpdate)).getText());
        when(mockCallbackQuery.getData()).thenReturn("postAll");
        assertEquals(MessageUtils.AWAITING_DATE,
                ((SendMessage) callBackQueriesHandler.processUpdate(mockUpdate)).getText());
        //Пишет бот если пост не ожидается, когда кнопка жмется в любое время
        when(userDataCache.getUsersBotState(anyLong())).thenReturn(BotState.READY_TO_WORK);
        when(mockCallbackQuery.getId()).thenReturn("asdf");
        assertEquals(MessageUtils.DONT_AWAIT_CONTENT,
                ((AnswerCallbackQuery) callBackQueriesHandler.processUpdate(mockUpdate)).getText());
    }
    /*Проверка нажатия пользователем кнопки Сохранить пост*/
    @Test
    void processUpdateCallBackQueriesPressSave(){
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallbackQuery);
        Message mockMessage = Mockito.mock(Message.class);
        when(mockCallbackQuery.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(1L);
        when(mockCallbackQuery.getFrom()).thenReturn(mockUser);
        setUpEnteringWhenContainsPostCreator(BotState.AWAITING_DATE);
        when(mockCallbackQuery.getData()).thenReturn("saveYes");
        when(mockCallbackQuery.getId()).thenReturn("asdf");
        assertEquals(MessageUtils.POST_SAVED,
                ((AnswerCallbackQuery) callBackQueriesHandler.processUpdate(mockUpdate)).getText());
        when(userDataCache.getUsersBotState(anyLong())).thenReturn(BotState.READY_TO_WORK);
        assertEquals(
                MessageUtils.DONT_AWAIT_CONTENT,
                ((AnswerCallbackQuery) callBackQueriesHandler.processUpdate(mockUpdate)).getText());
    }

    /*Настройка возврата постКреатора*/
    private void setUpEnteringWhenContainsPostCreator(BotState botState){
        when(userDataCache.getUsersBotState(anyLong())).thenReturn(botState);
        when(userDataCache.getPostCreator(anyLong())).thenReturn(mockCreator);
        Post mockPost = Mockito.mock(Post.class);
        when(mockCreator.getPost()).thenReturn(mockPost);
    }

}