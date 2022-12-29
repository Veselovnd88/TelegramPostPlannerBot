package ru.veselov.plannerBot.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import ru.veselov.plannerBot.model.content.TextEntity;
import ru.veselov.plannerBot.model.content.TextMessageEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@Disabled
class TextRepositoryTest {


    @Autowired
    private TextRepository textRepository;

    @Test
    void textRepositoryTest(){
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setType("text_link");
        messageEntity.setOffset(0);
        messageEntity.setLength(10);
        messageEntity.setUrl("http://localhost:8080");
        TextEntity text=new TextEntity("Hello world");
        TextMessageEntity tme =new TextMessageEntity();
        tme.setEntity(messageEntity);
        text.setEntities(List.of(tme));
        TextEntity save = textRepository.save(text);
        assertEquals(1,textRepository.findAll().size());
        assertTrue(textRepository.findById(save.getTextId()).isPresent());
        Optional<TextEntity> byId = textRepository.findById(save.getTextId());
        TextEntity text1 = byId.get();
        List<TextMessageEntity> entities = text1.getEntities();
        assertFalse(entities.isEmpty());
        textRepository.deleteAll();
    }

    @Test
    void textRepositoryWithoutEntityTest(){
        TextEntity text=new TextEntity("Hello world");
        TextEntity save = textRepository.save(text);
        assertEquals(1,textRepository.findAll().size());
        assertTrue(textRepository.findById(save.getTextId()).isPresent());
        Optional<TextEntity> byId = textRepository.findById(save.getTextId());
        TextEntity text1 = byId.get();
        List<TextMessageEntity> entities = text1.getEntities();
        assertTrue(entities.isEmpty());
        textRepository.deleteAll();
    }

}