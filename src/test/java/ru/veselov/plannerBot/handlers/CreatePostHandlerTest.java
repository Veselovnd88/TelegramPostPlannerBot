package ru.veselov.plannerBot.handlers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.UserDataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.controller.handlers.CreatePostHandler;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Slf4j
class CreatePostHandlerTest {
    @MockBean
    private MyPreciousBot bot;

    @Autowired
    private UserService userService;
    @Autowired
    private UserDataCache userDataCache;
    @Autowired
    private CreatePostHandler createPostHandler;
    private final Update mockUpdate = Mockito.mock(Update.class);
    private final Message mockMessage = Mockito.mock(Message.class);
    private final User mockUser = Mockito.mock(User.class);
    private final Chat mockChat = Mockito.mock(Chat.class);
    @BeforeAll
    static void init(){
        log.info("Тестирование Обработчика для создания постов");
    }
    @BeforeEach
    public void setMocks(){
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getFrom()).thenReturn(mockUser);
        when(mockChat.getTitle()).thenReturn("Mock Chat Title");

    }

    /*Тестирование на готовность ввода текста*/
    @Test
    void processUpdateTextInput(){
        when(mockMessage.hasText()).thenReturn(true);
        for(int i=100; i<120; i++){
            when(mockUser.getId()).thenReturn(Long.valueOf(i));
            userService.save(mockChat,mockUser);
            userDataCache.createPostCreator(mockUser);
            userDataCache.setUserBotState(mockUser.getId(), BotState.AWAITING_POST);
            when(mockMessage.getText()).thenReturn("Value "+ i);
            createPostHandler.processUpdate(mockUpdate);
            userService.removeUser(mockUser);
            assertEquals(0,userService.findAllChatsByUser(mockUser).size());
        }
        assertEquals(20, userDataCache.getPostCreators().size());

    }
    /*Тестирование на готовность ввода картинки*/
    @Test
    void processUpdatePictureInput(){
        when(mockMessage.hasPhoto()).thenReturn(true);
        PhotoSize mockPhotoSize = mock(PhotoSize.class);
        when(mockPhotoSize.getFileId()).thenReturn("photo_id");
        List<PhotoSize> photoSizeList = new ArrayList<>();
        photoSizeList.add(mockPhotoSize);
        when(mockMessage.getPhoto()).thenReturn(photoSizeList);
        for(int i=100; i<120;i++){
            when(mockUser.getId()).thenReturn(Long.valueOf(i));
            userService.save(mockChat,mockUser);
            userDataCache.createPostCreator(mockUser);
            userDataCache.setUserBotState(mockUser.getId(), BotState.AWAITING_POST);
            createPostHandler.processUpdate(mockUpdate);
            assertEquals(1,userDataCache.getPostCreator((long) i).getPost().getPhotos().size());
            assertEquals(0,userDataCache.getPostCreator((long)i).getPost().getTexts().size());

        }
        when(mockMessage.getCaption()).thenReturn("My test caption");
        for(int i=100; i<120;i++){
            when(mockUser.getId()).thenReturn(Long.valueOf(i));
            createPostHandler.processUpdate(mockUpdate);
            assertEquals(2,userDataCache.getPostCreator((long) i).getPost().getPhotos().size());
            userService.removeUser(mockUser);
        }
        assertEquals(0,userService.findAllChatsByUser(mockUser).size());
        assertEquals(20,userDataCache.getPostCreators().size());
    }



    @Test
    void processUpdateButtonsAppears(){
        when(mockUser.getId()).thenReturn(1L);
        userService.save(mockChat,mockUser);
        when(mockMessage.getText()).thenReturn("Value "+String.valueOf(1L));
        SendMessage receivedMessage = createPostHandler.processUpdate(mockUpdate);
        assertEquals(MessageUtils.AWAIT_CONTENT_MESSAGE,receivedMessage.getText());
        assertNotNull(receivedMessage.getReplyMarkup());
    }

}