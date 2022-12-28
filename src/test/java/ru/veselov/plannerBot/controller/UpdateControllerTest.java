package ru.veselov.plannerBot.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.BotState;
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
import java.util.List;

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
            runAllActions(user,date);
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
    void checkStates(){
        user.setId(-105L);
        //старт-сброс
        userPressStart(user);//старт, каналов нет
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
        userCreatePost(user);//создать пост, но каналов нет
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
        userReset(user);//сброс, каналов нет
        assertEquals(BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL,userDataCache.getUsersBotState(user.getId()));
        //Добавили в канал - начали создавать пост-сбросили
        userAddToChannels(user); //добавили в канал
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        userCreatePost(user);//создаем пост
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userReset(user);
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        assertEquals(0,userDataCache.getPostCreators().size());
        /*Сброс после добавления текста*/
        userCreatePost(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userReset(user);
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        assertEquals(0,userDataCache.getPostCreators().size());
        /*Сброс после выбора каналов*/
        userCreatePost(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userPressButtonForChoseChanel(user);
        assertEquals(BotState.AWAITING_DATE,userDataCache.getUsersBotState(user.getId()));
        userReset(user);
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        assertEquals(0,userDataCache.getPostCreators().size());
        /*Сброс после ввода даты
        * */
        userCreatePost(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userPressButtonForChoseChanel(user);
        assertEquals(BotState.AWAITING_DATE,userDataCache.getUsersBotState(user.getId()));
        userInputDate(user, "19.12.2022 16 00");
        assertEquals(BotState.READY_TO_SAVE, userDataCache.getUsersBotState(user.getId()));
        userReset(user);
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        assertEquals(0,userDataCache.getPostCreators().size());
        /* Бот добавлен в канал - начали создавать пост, отправили текст 2 раза, выбрали канал,
        ввели дату, нажали сохранить
         */
        userCreatePost(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userSendText(user);
        assertEquals(BotState.AWAITING_POST,userDataCache.getUsersBotState(user.getId()));
        userPressButtonForChoseChanel(user);
        assertEquals(BotState.AWAITING_DATE,userDataCache.getUsersBotState(user.getId()));
        userInputDate(user, "19.12.2022 16 00");
        assertEquals(BotState.READY_TO_SAVE, userDataCache.getUsersBotState(user.getId()));
        userPressButtonSave(user);
        assertEquals(BotState.READY_TO_WORK,userDataCache.getUsersBotState(user.getId()));
        //удаление за собой
        userService.removeUser(user);
    }


    /*Два пользователя рандомно создают посты*/
    @Test
    void usersRandomActions(){
        String date = "19.12.2022 16 00";
        userPressStart(user);
        userPressStart(user2);
        userAddToChannels(user);
        userAddToChannels(user2);
        Assertions.assertEquals(1,userService.findAllChatsByUser(user).size());
        Assertions.assertEquals(1,userService.findAllChatsByUser(user2).size());
        userCreatePost(user);
        userCreatePost(user2);
        userSendText(user);
        userPressButtonForChoseChanel(user);
        userSendText(user2);
        userPressButtonForChoseChanel(user2);
        userInputDate(user2,date);
        userInputDate(user,date);
        userPressButtonSave(user);
        userPressButtonSave(user2);
        Assertions.assertEquals(1,postService.findByUser(user).size());
        Assertions.assertEquals(1,postService.findByUser(user2).size());
        assertEquals(0,userDataCache.getPostCreators().size());
        //Удаление за собой
        userService.removeUser(user);
        userService.removeUser(user2);
    }


    /*Пользователь два раза подряд жмет кнопку Сохранить*/
    @Test
    void twoTimesPressSave(){
        user.setId(-101L);
        String date ="19.11.2022 16 00";
        userAddToChannels(user);
        userCreatePost(user);
        userSendText(user);
        userPressButtonForChoseChanel(user);
        userInputDate(user,date);
        userPressButtonSave(user);
        userPressButtonSave(user);
        assertEquals(0,userDataCache.getPostCreators().size());
        //Удаление за собой
        userService.removeUser(user);
    }

    private void runAllActions(User user,String date){
        userPressStart(user);
        userAddToChannels(user);
        userCreatePost(user);
        userSendText(user);
        userSendText(user);
        userPressButtonForChoseChanel(user);
        userInputDate(user,date);
        userPressButtonSave(user);
    }




    private void userPressStart(User user){
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.getText()).thenReturn("/start");
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        updateController.processUpdate(mockUpdate);
    }

    private void userAddToChannels(User user) {
        try {
            botId = bot.getMe().getId();
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(false);
        when(mockUpdate.hasMyChatMember()).thenReturn(true);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getFrom()).thenReturn(user);
        when(chatMemberUpdated.getNewChatMember()).thenReturn(mockChatMember);
        when(chatMemberUpdated.getFrom()).thenReturn(user);
        when(chatMemberUpdated.getChat()).thenReturn(chat);
        when(mockUpdate.getMyChatMember()).thenReturn(chatMemberUpdated);
        when(mockChatMember.getUser()).thenReturn(user);
        when(chatMemberUpdated.getNewChatMember()).thenReturn(mockChatMember);
        when(mockChatMember.getUser()).thenReturn(mockBotUser);
        when(mockBotUser.getId()).thenReturn(botId);
        when(mockChatMember.getStatus()).thenReturn("administrator");
        updateController.processUpdate(mockUpdate);
    }

    private void userCreatePost(User user){
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
        updateController.processUpdate(mockUpdate);
    }

    private void userSendText(User user){
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn("Тестовый текст");
        updateController.processUpdate(mockUpdate);
    }

    private void userPressButtonForChoseChanel(User user){
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(false);
        when(mockUpdate.hasCallbackQuery()).thenReturn(true);
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallBack);
        when(mockUpdate.getCallbackQuery().getId()).thenReturn("1");
        when(mockCallBack.getData()).thenReturn("test1");
        when(mockCallBack.getFrom()).thenReturn(user);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockCallBack.getMessage()).thenReturn(mockMessage);
        updateController.processUpdate(mockUpdate);
    }

    private void userInputDate(User user,String date){
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("_");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn(date);
        updateController.processUpdate(mockUpdate);
    }

    private void userPressButtonSave(User user){
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(false);
        when(mockUpdate.hasCallbackQuery()).thenReturn(true);
        when(mockUpdate.getCallbackQuery()).thenReturn(mockCallBack);
        when(mockCallBack.getData()).thenReturn("saveYes");
        when(mockCallBack.getFrom()).thenReturn(user);
        when(mockCallBack.getMessage()).thenReturn(mockMessage);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        updateController.processUpdate(mockUpdate);
    }

    private void userReset(User user){
        when(mockUpdate.hasCallbackQuery()).thenReturn(false);
        when(mockUpdate.hasMessage()).thenReturn(true);
        when(mockUpdate.hasMyChatMember()).thenReturn(false);
        when(mockMessage.getFrom()).thenReturn(user);
        when(mockMessage.hasText()).thenReturn(true);
        when(mockMessage.getChatId()).thenReturn(user.getId());
        when(mockMessage.getText()).thenReturn("/reset");
        when(mockMessage.hasEntities()).thenReturn(true);
        when(mockEntity.getType()).thenReturn("bot_command");
        when(mockMessage.getEntities()).thenReturn(List.of(mockEntity));
        updateController.processUpdate(mockUpdate);
    }

}
