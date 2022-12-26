package ru.veselov.plannerBot.controller;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.*;

import java.util.List;

import static org.mockito.Mockito.when;

public class UserActions {
    public Update mockUpdate= Mockito.mock(Update.class);
    public Message mockMessage=Mockito.mock(Message.class);
    public MessageEntity mockEntity= Mockito.mock(MessageEntity.class);
    public CallbackQuery mockCallBack=Mockito.mock(CallbackQuery.class);

    public UserActions(){
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
    }
    public Update userPressStart(User user){
        /*Пользователь жмет старт*/
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getText()).thenReturn("/start");
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        return mockUpdate;
    }

    public Update userCreatePost(User user){
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(true);
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
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn("Тестовый текст");
        return mockUpdate;
    }

    public Update userPressButtonForChoseChanel(User user, String chatName){
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(false);
        when(mockUpdate.hasCallbackQuery()).thenReturn(true);
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallBack);
        when(mockUpdate.getCallbackQuery().getId()).thenReturn("1");
        when(mockCallBack.getData()).thenReturn(chatName);
        when(mockCallBack.getFrom()).thenReturn(user);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockCallBack.getMessage()).thenReturn(mockMessage);
        return mockUpdate;
    }
}
