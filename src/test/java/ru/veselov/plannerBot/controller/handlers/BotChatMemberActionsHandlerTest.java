package ru.veselov.plannerBot.controller.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SpringBootTest
class BotChatMemberActionsHandlerTest {
    @Autowired
    private BotChatMemberActionsHandler botChatMemberActionsHandler;
    @Autowired
    DataCache userDataCache;
    private Update mockUpdate = spy(Update.class);
    private CallbackQuery mockCallback = spy(CallbackQuery.class);
    private User mockUser = spy(User.class);
    public ChatMemberUpdated chatMemberUpdated = spy(ChatMemberUpdated.class);
    public ChatMember chatMember = spy(ChatMember.class);
    public Chat mockChat = spy(Chat.class);


    @MockBean
    UserService userService;
    @MockBean
    PostService postService;

    @MockBean
    MyPreciousBot bot;

    @BeforeEach
    void init() {
        when(mockUpdate.getMyChatMember()).thenReturn(chatMemberUpdated);
        when(chatMemberUpdated.getNewChatMember()).thenReturn(chatMember);
        when(chatMemberUpdated.getFrom()).thenReturn(mockUser);
        when(chatMemberUpdated.getChat()).thenReturn(mockChat);
        when(chatMember.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(1L);
        when(bot.getMyId()).thenReturn(1L);
        when(mockChat.getTitle()).thenReturn("111");
        when(mockChat.getId()).thenReturn(-100L);

    }
    @Test
    void addBotAsAdminStatusChangingTest(){
        //Бота добавили в канал как Администратора
        when(chatMember.getStatus()).thenReturn("administrator");
        userDataCache.setUserBotState(mockUser.getId(), BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
        botChatMemberActionsHandler.processUpdate(mockUpdate);
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(mockUser.getId()));
    }
    @Test
    void removeBotFromAdminStatusChangingTest(){
        //Бота удалили из единственного канала (у пользователя больше нет каналов)
        when(userService.findUsersWithChat(mockChat.getId().toString())).thenReturn(Map.of(1L,1));
        for(String status: List.of("left","kicked")){
        when(chatMember.getStatus()).thenReturn(status);
        for(BotState s: BotState.values()){
            userDataCache.setUserBotState(mockUser.getId(), s);
            botChatMemberActionsHandler.processUpdate(mockUpdate);
            assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(mockUser.getId()));
        }
        }
    }
    @Test
    void removeBotFromAdminOneChatStatusChangingTest(){
        //Бота удалили из единственного канала (у пользователя больше нет каналов)
        when(userService.findUsersWithChat(mockChat.getId().toString())).thenReturn(Map.of(1L,1));
        for(String status: List.of("left","kicked")){
            when(chatMember.getStatus()).thenReturn(status);
            for(BotState s: BotState.values()){
                userDataCache.setUserBotState(mockUser.getId(), s);
                botChatMemberActionsHandler.processUpdate(mockUpdate);
                assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(mockUser.getId()));
            }
        }
    }
    @Test
    void removeBotFromAdminMoreThanOneChatStatusChangingTest(){
        //Бота удалили из единственного канала (у пользователя больше нет каналов)
        when(userService.findUsersWithChat(mockChat.getId().toString())).thenReturn(Map.of(1L,2));
        for(String status: List.of("left","kicked")){
            when(chatMember.getStatus()).thenReturn(status);
            for(BotState s: BotState.values()){
                userDataCache.setUserBotState(mockUser.getId(), s);
                botChatMemberActionsHandler.processUpdate(mockUpdate);
                assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(mockUser.getId()));
            }
        }
    }
}