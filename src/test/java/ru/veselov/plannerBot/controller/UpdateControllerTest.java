package ru.veselov.plannerBot.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.UserDataCache;
import ru.veselov.plannerBot.controller.handlers.CommandMenuHandler;
import ru.veselov.plannerBot.controller.handlers.CreatePostHandler;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.repository.PostRepository;
import ru.veselov.plannerBot.repository.TextRepository;
import ru.veselov.plannerBot.repository.UserRepository;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.BotProperties;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Disabled
public class UpdateControllerTest {

    @Autowired
    private MyPreciousBot bot;
    @Autowired
    private BotProperties botProperties;
    @Autowired
    TelegramBotsApi telegramBotsApi;
    @Autowired
    private UserService userService;
    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TextRepository textRepository;
    @Autowired
    private UserDataCache userDataCache;
    @Autowired
    private CreatePostHandler createPostHandler;
    @Autowired
    private CommandMenuHandler commandMenuHandler;
    @Autowired
    private UpdateController updateController;


    /*Тестирование полного функционала от нескольких пользователей*/
    @Test
    void userWorkFlowTest(){
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for(int i=0; i<100; i++){
            User user1=new User();
            user1.setId(-100L- (long) i);
            user1.setFirstName("Vasya "+i);
            user1.setLastName("Petya "+ i);
            user1.setUserName("ZloyPetya "+i);
            Runnable task=new Runnable() {
                @Override
                public void run() {
                    System.out.println("Поток go");
                    workFlow(user1);
                    assertNotNull(userService.findAllChatsByUser(user1));
                    assertEquals(1,postService.findByUser(user1).size());
                    PostEntity post = postService.findByUser(user1).get(0);
                    assertEquals(PostState.SAVED, post.getPostState());
                    userService.removeUser(user1);
                }
            };
            executorService.execute(task);
        }
        executorService.shutdown();
        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private  void workFlow(User user){
        UserActions userActions = new UserActions();
        updateController.processUpdate(userActions.userPressStart(user));
        updateController.processUpdate(userActions.botAndChannelAction("administrator",user, bot.getMyId()));
        updateController.processUpdate(userActions.userCreatePost(user));
        updateController.processUpdate(userActions.userPressButtonForChoseChanel(user,"postAll"));
        updateController.processUpdate(userActions.userInputDate(user,"31.12.2022 15 00"));
        updateController.processUpdate(userActions.userSavedDate(user));
    }

}
