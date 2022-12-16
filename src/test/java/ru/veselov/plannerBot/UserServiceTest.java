package ru.veselov.plannerBot;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.repository.PostRepository;
import ru.veselov.plannerBot.repository.UserRepository;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

@SpringBootTest
public class UserServiceTest {
    @Autowired
    private UserService userService;
    @Autowired
    private PostService postService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private PostRepository postRepository;

    @Test
    @Transactional
    @Disabled
    public void saveAndDeleteChats(){
        User user = new User();
        user.setId(111111L);
        User user2 = new User();
        user2.setId(2222L);
        User user3 = new User();
        user3.setId(3333L);
        //3 добавляются один чат
        Chat chat = new Chat();
        chat.setId(666666L);
        userService.save(chat,user);//пользователь добавил бота в 1 чат
        userService.save(chat,user2);
        userService.save(chat,user3);//3 пользователя добавили бота в чат
        Chat chat2 = new Chat();
        chat2.setId(5555L);
        Chat chat3 = new Chat();
        chat3.setId(5556L);
        userService.save(chat2,user);//пользователь добавит бота во 2 чат
        userService.save(chat3,user3);//у юзера 1 - 2 чата, у юзера 3 - 2 чата
        Optional<UserEntity> optional = userRepository.findByUserId("111111");
        Assertions.assertEquals(2,optional.get().getChats().size());
        Assertions.assertEquals(3,userRepository.findAll().size());
        Assertions.assertEquals(3, chatRepository.findAll().size());//два других пользователя остались присоединены к чату 1
        userService.removeChat(chat.getId().toString());//удалили бота из одного чата - чат удалился у всех юзеров
        Assertions.assertEquals(1,optional.get().getChats().size());
        Assertions.assertEquals(2, chatRepository.findAll().size());
        userService.removeUser(user3);
        userService.removeUser(user);//удалили пользователя, за ним удаляются все чаты
        userService.removeUser(user2);
        Assertions.assertEquals(0,userRepository.findAll().size());
        Assertions.assertEquals(0, chatRepository.findAll().size());
    }


    @Test
    @Disabled
    @Transactional
    public void withPost(){
        User user = new User();
        user.setId(111111L);
        Chat chat = new Chat();
        chat.setId(666666L);
        userService.save(chat,user);//пользователь добавил бота в 1 чат
        Post post = new Post();
        Set<Chat> allChatsByUser = userService.findAllChatsByUser(user);
        post.setUser(user);
        post.setChats(allChatsByUser);
        post.setPostState(PostState.CREATED);
        post.setDate(new Date());
        postService.planPost(post);
        Optional<UserEntity> optional = userRepository.findByUserId("111111");

        Assertions.assertEquals(1,optional.get().getChats().size());
        Assertions.assertEquals(1, chatRepository.findAll().size());//два других пользователя остались присоединены к чату 1
        userService.removeChat(chat.getId().toString());//удалили бота из одного чата
        Assertions.assertEquals(1,postRepository.findAll().size());
        Assertions.assertEquals(0,optional.get().getChats().size());
        Assertions.assertEquals(0, chatRepository.findAll().size());
        userService.removeUser(user);
        Assertions.assertEquals(0,userRepository.findAll().size());
    }

}