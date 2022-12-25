package ru.veselov.plannerBot.controller;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.List;

import static org.mockito.Mockito.when;

public class UserActions {
    public Update mockUpdate= Mockito.mock(Update.class);
    public Message mockMessage=Mockito.mock(Message.class);
    public MessageEntity mockEntity= Mockito.mock(MessageEntity.class);
    public Update userPressStart(User user){
        /*Пользователь жмет старт*/
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
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

    private Update userCreatePost(User user){
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
}
