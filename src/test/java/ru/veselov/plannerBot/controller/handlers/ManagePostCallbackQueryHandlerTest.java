package ru.veselov.plannerBot.controller.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.postsender.PostSender;
import ru.veselov.plannerBot.utils.Utils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
@SpringBootTest
class ManagePostCallbackQueryHandlerTest {

    private Update mockUpdate = spy(Update.class);
    private CallbackQuery mockCallback = spy(CallbackQuery.class);
    private User mockUser = spy(User.class);
    @Autowired
    private DataCache userDataCache;
    @MockBean
    private PostService postService;
    @MockBean
    private PostSender postSender;

    @Autowired
    private Utils utils;
    @Autowired
    private ManagePostCallbackQueryHandler managePostCallbackQueryHandler;
    @BeforeEach
    void init(){
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallback);
        when(mockCallback.getFrom()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);
    }
    @Test
    void processUpdateReturnMessageWithRemoveKeyboardTest() {
        for(String s :List.of("delete", "view", "send")){
            when(mockCallback.getData()).thenReturn("s");
            BotApiMethod<?> botApiMethod = managePostCallbackQueryHandler.processUpdate(mockUpdate);
            assertTrue((botApiMethod instanceof SendMessage));
            assertInstanceOf(ReplyKeyboardRemove.class, ((SendMessage) botApiMethod).getReplyMarkup());}
    }
}