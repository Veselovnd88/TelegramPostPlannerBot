package ru.veselov.plannerBot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.UserDataCache;
import ru.veselov.plannerBot.controller.handlers.CommandMenuHandler;
import ru.veselov.plannerBot.controller.handlers.CreatePostHandler;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.repository.PostRepository;
import ru.veselov.plannerBot.repository.TextRepository;
import ru.veselov.plannerBot.repository.UserRepository;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.BotProperties;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@SpringBootTest
@Disabled
public class UpdateControllerTest {
    private Long botId = 0L;//тестируется с реальным ID бота

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
    public Update mockUpdate = Mockito.mock(Update.class);
    public Message mockMessage = Mockito.mock(Message.class);
    public User mockUser = Mockito.mock(User.class);
    public User mockBotUser = Mockito.mock(User.class);
    public Chat chat;
    public SimpleDateFormat sdf;
    public User user;
    public User user2;
    public Post post;
    public UserEntity userEntity;
    public ChatMemberUpdated chatMemberUpdated = Mockito.mock(ChatMemberUpdated.class);
    public ChatMember mockChatMember = Mockito.mock(ChatMember.class);
    public CallbackQuery mockCallBack = Mockito.mock(CallbackQuery.class);
    public MessageEntity mockEntity = Mockito.mock(MessageEntity.class);



    @BeforeEach
    void setUp(){
        sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");
        user=new User();
        user.setId(-100L);
        user.setFirstName("Vasya");
        user.setLastName("Petya");
        user.setUserName("ZloyPetya");
        user2=new User();
        user2.setId(-200L);
        user2.setFirstName("Pupka");
        user2.setLastName("Zloba");
        user2.setUserName("Belzebub");
        post=new Post();
        chat=new Chat();
        chat.setId(1111L);
        chat.setTitle("test1");
        when(mockUpdate.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChat()).thenReturn(chat);
    }
    /*Тестирование полного функционала от нескольких пользователей*/

    @Test
    void usersPostsTest(){
        String date = "06.12.2022 16 00";//проверять дату перед тестом
        int tests = 2;
        for(int i=0; i<tests; i++) {
            Long userId = i-1000L;
            user.setId(userId);
            assertNotNull(userService.findAllChatsByUser(user));
            assertEquals(1,postService.findByUser(user).size());
            PostEntity post = postService.findByUser(user).get(0);
            assertEquals(PostState.PLANNED, post.getPostState());
            //Удаление после тестов
            userService.removeUser(user);
        }
        assertEquals(0,userDataCache.getPostCreators().size());
    }
    /////////////////
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
        //workFlow(user);
        /*assertNotNull(userService.findAllChatsByUser(user));
        assertEquals(1,postService.findByUser(user).size());
        PostEntity post = postService.findByUser(user).get(0);
        assertEquals(PostState.SAVED, post.getPostState());
        userService.removeUser(user);*/
    }


    /*Два пользователя рандомно создают посты*/


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
