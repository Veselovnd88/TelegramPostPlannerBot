package ru.veselov.plannerBot.service.postsender;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
class PostSenderTest {
    @MockBean
    MyPreciousBot bot;
    @Autowired
    PostSender postSender;

    private Post post;
    private Post post2;
    private Post post3;
    private Chat chat;
    private Chat chat2;
    @BeforeEach
    void init(){
        post = new Post();
        post.setDate(new Date());
        chat = new Chat();
        chat.setId(1L);
        chat.setTitle("Test");
        post.setChats(Set.of(chat));
        chat2=new Chat();
        chat2.setId(2L);
        chat2.setTitle("Test2");

        post2 = new Post();
        post2.setDate(new Date());
        post2.setChats(Set.of(chat));
        post3=new Post();
        post3.setDate(new Date());
        post3.setChats(Set.of(chat2));
    }
    @Test
    void sendTestIntervals() {
/*Проверяет, отправление постов не нарушает ограничение по отправке 30 постов в сек*/
        int quantity=50;
        for(int i=0; i<quantity; i++){
            Message message = new Message();
            message.setText("Text "+i);
            post.getMessages().add(message);
        }
        Date before = new Date();
        try {
            postSender.send(post);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        Date after = new Date();
        assertTrue((after.getTime()-before.getTime())>(1000/30*quantity));
    }

    @Test
    void sendSeveralChats(){
        /*Отправка сообщений не реже 20 постов в минуту*/
        int quantity=20;
        for(int i=0; i<quantity; i++){
            Message message = new Message();
            message.setText("Text");
            post.getMessages().add(message);
            post2.getMessages().add(message);
            post3.getMessages().add(message);
        }
        Date before = new Date();
        try {
            postSender.send(post);
            postSender.send(post2);
            postSender.send(post3);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        try {
            Thread.sleep(16000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Mockito.verify(bot,Mockito.times(40)).sendMessageBot(
                SendMessage.builder().chatId(chat.getId())
                        .text("Text").build());
        Mockito.verify(bot,Mockito.times(20)).sendMessageBot(
                SendMessage.builder().chatId(chat2.getId())
                        .text("Text").build());
        Date after = new Date();
        System.out.println(after.getTime()-before.getTime());
        assertTrue((after.getTime()-before.getTime())<quantity*3*150+16000);

    }
}