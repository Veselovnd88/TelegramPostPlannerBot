package ru.veselov.plannerBot.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.content.MessageDBEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Disabled
class PostRepositoryTest {
    @Autowired
    ModelMapper modelMapper;

    @Autowired
    PostRepository postRepository;

    @Test
    void savePostTest(){
        PostEntity post = new PostEntity();
        Message message = new Message();
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setType("text_link");
        messageEntity.setOffset(0);
        messageEntity.setLength(10);
        messageEntity.setUrl("http://localhost:8080");
        message.setEntities(List.of(messageEntity));
        message.setText("hello world everybody");
        MessageDBEntity messageDBEntity = new MessageDBEntity();
        messageDBEntity.setMessage(message);
        post.addMessage(messageDBEntity);
        postRepository.save(post);
        postRepository.deleteAll();

    }

}