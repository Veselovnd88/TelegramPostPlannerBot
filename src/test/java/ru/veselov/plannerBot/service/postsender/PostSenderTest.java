package ru.veselov.plannerBot.service.postsender;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;

import java.util.Date;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class PostSenderTest {
    @MockBean
    MyPreciousBot bot;
    @Autowired
    PostSender postSender;
    @Test
    void send() {
        Post post = new Post();
        post.setDate(new Date());
        Chat chat = new Chat();
        chat.setId(1L);
        chat.setTitle("Test");
        post.setChats(Set.of(chat));
        for(int i=0; i<50; i++){
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
        System.out.println(after.getTime()-before.getTime());

    }
}