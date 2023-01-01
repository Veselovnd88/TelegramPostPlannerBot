package ru.veselov.plannerBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.model.content.ChatEntity;
import ru.veselov.plannerBot.model.content.MessageDBEntity;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.repository.PostRepository;
import ru.veselov.plannerBot.repository.UserRepository;
import ru.veselov.plannerBot.service.postsender.PostSender;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserService userService;
    private final PostSender postSender;

    @Autowired
    public PostService(PostRepository postRepository, UserRepository userRepository, ChatRepository chatRepository, UserService userService, PostSender postSender) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.userService = userService;
        this.postSender = postSender;
    }
    /*Планировщик постов, сюда попадает ДТО в случаях:
    * 1) Сразу после создания - со статусом CREATE, проверяется время публикации внутри метода
    * 2) Из ScheduledService - при выборке из БД с условием SAVED и не позднее +1 день после старта*/
    @Transactional
    public void planPost(Post postDto){
        Calendar cl=Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        PostState postState = postDto.getPostState();
        User user = postDto.getUser();
        cl.add(Calendar.DATE,1);
        //Для связки юзера и поста - получаем из бд
        Optional<UserEntity> userOptional = userRepository.findByUserId(user.getId().toString());
        //Если юзер существует, то сразу проверяем дату поста
        if(userOptional.isPresent()){
            PostEntity postEntity;
            if(postDto.getDate().before(cl.getTime())){
                if(postState==PostState.CREATED){
                    postEntity=convertToEntity(postDto);
                    postEntity.setPostState(PostState.PLANNED);
                }
                else if(postState==PostState.SAVED){
                    Optional<PostEntity> optionalPostEntity = postRepository.findById(postDto.getPostId());
                    postEntity = optionalPostEntity.orElseGet(() -> convertToEntity(postDto));
                    postEntity.setPostState(PostState.PLANNED);
                }
                else if(postState==PostState.PLANNED){
                    Optional<PostEntity> optionalPostEntity =postRepository.findById(postDto.getPostId());
                    if (optionalPostEntity.isPresent()){
                        postEntity = optionalPostEntity.get();
                        postEntity.setDate(postDto.getDate());
                    }
                    else{
                        postEntity=convertToEntity(postDto);
                    }

                }
                else{
                    postEntity=convertToEntity(postDto);
                    postEntity.setPostState(PostState.PLANNED);
                }
            }else{
                postEntity=convertToEntity(postDto);
                postEntity.setPostState(PostState.SAVED);
            }
            //Устанавливаем юзера к посту, и добавляем пост к юзеру
            userOptional.get().addPost(postEntity);
            PostEntity savedPost = postRepository.save(postEntity);
            if(savedPost.getPostState()==PostState.PLANNED){
                postSender.createTimer(convertToPost(savedPost),this);
            }
        }
    }

    public void addChat(PostEntity post, Chat chat){
        Optional<ChatEntity> entity = chatRepository.findByChatId(chat.getId().toString());
        entity.ifPresent(post::addChat);
    }

    public List<Post> todayPosts(Date dateUntil, PostState postState){
        return postRepository.findByDateBeforeAndPostState(dateUntil, postState).stream()
                .map(this::convertToPost).toList();
    }

    public List<Post> getPostsByState(PostState postState){
        return postRepository.findByPostState(postState).stream()
                .map(this::convertToPost).toList();
    }

    @Transactional
    //Изменение статуса
    public void savePost(Post post){
        Optional<PostEntity> postEntity = postRepository.findById(post.getPostId());
        if(postEntity.isPresent()){
            PostEntity get = postEntity.get();
            get.setPostState(PostState.SENT);
            get.setDate(post.getDate());
            postRepository.save(get);
        }
    }
    public List<Post> findByUserAndPostStates(User user, List<PostState> postStates){
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(user.getId().toString());
        return postRepository.findByUserAndPostStateIn(
                userEntity,postStates).stream().map(this::convertToPost).toList();
    }

    public List<PostEntity> findByUser(User user){
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(user.getId().toString());
        return postRepository.findAllByUser(userEntity);
    }

    @Transactional
    public void deleteById(Integer id){
        postRepository.deleteById(id);
    }

    public Optional<Post> findById(Integer id) {
        return postRepository.findById(id).map(this::convertToPost);
    }
    public boolean existsByPostId(Integer id){
        return postRepository.existsByPostId(id);
    }
////////////////////////////////Конвертеры/////////////////////
 private PostEntity convertToEntity(Post post){
        PostEntity postEntity = new PostEntity();
        if(post.getPostId()!=0){
            postEntity.setPostId(post.getPostId());}
        //////
        for(Message m: post.getMessages()){
            MessageDBEntity messageDBEntity=new MessageDBEntity();
            messageDBEntity.setMessage(m);
            postEntity.addMessage(messageDBEntity);
        }
        ////
        for(var chat: post.getChats()){
            addChat(postEntity,chat);
        }
        postEntity.setDate(post.getDate());
        postEntity.setPostState(post.getPostState());
        return postEntity;
    }
    private Post convertToPost(PostEntity pe){
        Post post = new Post();
        post.setPostId(pe.getPostId());
        post.setUser(userService.entityToUser(pe.getUser()));
        post.setPostState(pe.getPostState());
        post.setPostId(pe.getPostId());
        post.setDate(pe.getDate());
        post.setMessages(pe.getMessages().stream().map(MessageDBEntity::getMessage).toList());
        post.setChats(pe.getChats().stream().map(userService::entityToChat).collect(Collectors.toSet()));
        return post;
    }
}
