package ru.veselov.plannerBot.repositories;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.repository.*;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

@SpringBootTest
@Slf4j
public class PostServiceTest {

    @Autowired
    private PostService postService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private PostRepository postRepository;

    private User user;
    private UserEntity userEntity;
    private Post post;
    private Chat chat1;
    private Chat chat2;
    private SimpleDateFormat sdf;
    private Message message;
    @Autowired
    private UserService userService;
    @BeforeEach
    public void init(){
        sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");
        user=new User();
        user.setId(-100L);
        user.setFirstName("Vasya");
        user.setLastName("Petya");
        user.setUserName("ZloyPetya");
        post=new Post();
        chat1=new Chat();
        chat1.setId(1111L);
        chat1.setTitle("test1");
        chat2=new Chat();
        chat2.setId(2222L);
        chat2.setTitle("test2");
        //Добавили юзеру два чата
        userService.save(chat1, user);
        userService.save(chat2, user);
        /*Создали пост*/
        userEntity = new UserEntity();
        userEntity.setUserId(user.getId().toString());
        userEntity.setFirstName(user.getFirstName());
        userEntity.setUserName(user.getUserName());
        userEntity.setLastName(user.getLastName());
        post.setUser(user);
        post.setDate(new Date());
        post.setChats(Set.of(chat1));
        message = new Message();
        message.setText("Text");
    }

    @Test
    @Disabled
    void planPostPlan() throws ParseException {
        PostState postState = PostState.CREATED;
        Date postDateActual = sdf.parse("05.12.2022 17 25");
        //----------------------------------------//
        post.setMessages(List.of(message));
        post.setPostState(postState);
        post.setDate(postDateActual);
        postService.planPost(post);
        List<PostEntity> allByUser = postRepository.findAllByUser(userEntity);
        Assertions.assertEquals(1,allByUser.size());
        Assertions.assertEquals(PostState.PLANNED,allByUser.get(0).getPostState());
        //---------убрали за собой
        userService.removeUser(user);
    }


    @Test
    @Disabled
    void planPostSave() throws ParseException {
        PostState postState = PostState.CREATED;
        Date postDateSaved = sdf.parse("05.12.2023 17 25");//через год
        //----------------------------------------/
        post.setMessages(List.of(message));
        post.setPostState(postState);
        post.setDate(postDateSaved);
        postService.planPost(post);
        List<PostEntity> allByUser = postRepository.findAllByUser(userEntity);
        Assertions.assertEquals(1,allByUser.size());
        Assertions.assertEquals(PostState.SAVED,allByUser.get(0).getPostState());
        //---------убрали за собой
        userService.removeUser(user);
    }

    @Test
    @Disabled
    void planPostGetSavedAndPlan() throws ParseException {
        PostState postState = PostState.SAVED;
        Date postDatePlanned = sdf.parse("05.12.2022 17 25");//через год
        //----------------------------------------//
        post.setMessages(List.of(message));
        post.setPostState(postState);
        post.setDate(postDatePlanned);
        postService.planPost(post);
        List<PostEntity> allByUser = postRepository.findAllByUser(userEntity);
        Assertions.assertEquals(1,allByUser.size());
        Assertions.assertEquals(PostState.PLANNED,allByUser.get(0).getPostState());
        //---------убрали за собой
        userService.removeUser(user);
    }

    @Test
    @Disabled
    void planPostGetSavedAndStaySaved() throws ParseException {//с такой датой сюда не пост просто не должен попадать
        PostState postState = PostState.SAVED;
        Date postDatePlanned = sdf.parse("05.12.2023 17 25");//через год
        //----------------------------------------//
        post.setMessages(List.of(message));
        post.setPostState(postState);
        post.setDate(postDatePlanned);
        postService.planPost(post);
        List<PostEntity> allByUser = postRepository.findAllByUser(userEntity);
        Assertions.assertEquals(1,allByUser.size());
        Assertions.assertEquals(PostState.SAVED,allByUser.get(0).getPostState());
        //---------убрали за собой
        userService.removeUser(user);
    }

    @Test
    @Disabled
    public void testService(){
        post.setPostState(PostState.CREATED);
        postService.planPost(post);
        Assertions.assertEquals(1,postRepository.findAll().size());
        Assertions.assertEquals(2,chatRepository.findAll().size());
        //Просто удаляем юзера, без удаления чата
        userService.removeUser(user);
        Assertions.assertEquals(0,postRepository.findAll().size());
        Assertions.assertEquals(0,chatRepository.findAll().size());
    }

}
