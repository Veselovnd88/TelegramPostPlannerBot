package ru.veselov.plannerBot.controller;

import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;

import java.util.List;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class UserActions {
    public Update mockUpdate= Mockito.mock(Update.class);
    public Message mockMessage=Mockito.mock(Message.class);
    public MessageEntity mockEntity= Mockito.mock(MessageEntity.class);
    public CallbackQuery mockCallBack=spy(CallbackQuery.class);
    private User mockUser = spy(User.class);
    public ChatMemberUpdated chatMemberUpdated = spy(ChatMemberUpdated.class);
    public ChatMember chatMember = spy(ChatMember.class);
    public Chat mockChat = spy(Chat.class);

    public UserActions(){
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
    }
    public Update userPressStart(User user){
        /*Пользователь жмет /start*/
        setUpUpdateMessage();
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getText()).thenReturn("/start");
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        return mockUpdate;
    }

    public Update userCreatePost(User user){
        /*Пользователь жмет /create*/
        setUpUpdateMessage();
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn("/create");

        return mockUpdate;
    }

    public Update userSendText(User user){
        /*Пользователь вводит текст*/
        setUpUpdateMessage();
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn("Тестовый текст");
        return mockUpdate;
    }

    public Update userPressButtonForChoseChanel(User user, String chatName){
        /*Пользователь выбирает каналы*/
        callbackUpdate(chatName,user);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockCallBack.getMessage()).thenReturn(mockMessage);
        return mockUpdate;
    }

    public Update userInputDate(User user,String date){
        /*Пользователь вводит дату(вручную)*/
        setUpUpdateMessage();
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("_");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn(date);
        return mockUpdate;
    }

    public Update userSavedDate(User user){
        /*Пользователь жмет сохранить после ввода даты*/
        return callbackUpdate("saveYes",user);
    }

    public Update userInputDateAgain(User user){
        /*Пользователь нажимает ввести дату снова*/
        when(mockCallBack.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        return callbackUpdate("inputDate",user);
    }

    public Update userReset(User user){
        /*Пользователь жмет /reset*/
        setUpUpdateMessage();
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn("/reset");
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        return mockUpdate;
    }

    public Update userPressHelp(User user){
        /*Пользователь жмет /help*/
        setUpUpdateMessage();
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getText()).thenReturn("/help");
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        return mockUpdate;
    }

    public Update userPressView(User user){
        /*Пользователь жмет /view*/
        setUpUpdateMessage();
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getText()).thenReturn("/view");
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        return mockUpdate;
    }

    public Update adminPressPromote(User user){
        /*Пользователь жмет /promote */
        setUpUpdateMessage();
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getText()).thenReturn("/promote");
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        return mockUpdate;
    }

    public Update adminPromoteUser(User user){
        /*Админ выбирает новый статус пользователя*/
        when(mockCallBack.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        return callbackUpdate("premium",user);
    }

    public Update userPressManageCallback(User user){
        /*Пользователь жмет кнопку управления постами*/
        callbackUpdate("manage",user);
        return mockUpdate;
    }

    public Update userManagePost(User user){
        /*Пользователь жмет команду управления постами*/
        return callbackUpdate("delete",user);
    }

    public Update botAndChannelAction(String status, User user, Long botId){
        when(mockUpdate.hasMyChatMember()).thenReturn(true);
        when(mockUpdate.getMyChatMember()).thenReturn(chatMemberUpdated);
        when(chatMemberUpdated.getNewChatMember()).thenReturn(chatMember);
        when(chatMemberUpdated.getFrom()).thenReturn(user);
        when(chatMemberUpdated.getChat()).thenReturn(mockChat);
        when(chatMember.getUser()).thenReturn(mockUser);
        when(mockUser.getId()).thenReturn(botId);
        when(mockChat.getTitle()).thenReturn("111");
        when(mockChat.getId()).thenReturn(-100L);
        when(chatMember.getStatus()).thenReturn(status);
        return mockUpdate;
    }

    private Update callbackUpdate(String callbackData, User user){
        when(mockUpdate.hasCallbackQuery()).thenReturn(true);
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(false);
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallBack);
        when(mockCallBack.getData()).thenReturn(callbackData);
        when(mockCallBack.getId()).thenReturn("1");
        when(mockCallBack.getFrom()).thenReturn(user);
        return mockUpdate;
    }

    private void setUpUpdateMessage(){
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(true);
    }
}
