package ru.veselov.plannerBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.model.UserStatus;
import ru.veselov.plannerBot.model.content.ChatEntity;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.repository.PostRepository;
import ru.veselov.plannerBot.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final ChatRepository chatRepository;
    private final PostRepository postRepository;

    private final UserRepository userRepository;

    @Value("${bot.status.standard}")
    private Integer standard;
    @Value("${bot.status.premium}")
    private Integer premium;

    @Autowired
    public UserService(ChatRepository chatRepository, PostRepository postRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }


    @Transactional
    public void save(Chat chat, User user){
        //Берем из Бд нашего юзера, если он есть - то добавляем чат
        //Каскадно будут сохраняться чаты
        ChatEntity chatEntity;
        UserEntity userEntity;
        Optional<UserEntity> entityOptional = userRepository.findByUserId(user.getId().toString());
        Optional<ChatEntity> chatOptional = chatRepository.findByChatId(chat.getId().toString());
        /*Может существовать только одна сущность, поэтому - если в бд чата и юзера еще нет - то создаем новые*/
        chatEntity = chatOptional.orElseGet(() -> chatToEntity(chat));
        userEntity = entityOptional.orElseGet(() -> userToEntity(user));
        userEntity.addChat(chatEntity);
        chatEntity.addUser(userEntity);
        userRepository.save(userEntity);

        log.info("Канал {}: {} для пользователя {} сохранен в БД", chatEntity.getChatId(),
                chatEntity.getTitle(),user.getId());
    }
    /*Обновление данных пользователя*/
    @Transactional
    public void saveUser(UserEntity dto){
        Optional<UserEntity> byUserId = userRepository.findByUserId(dto.getUserId());
        if(byUserId.isPresent()){
            UserEntity userEntity = byUserId.get();
            userEntity.setStatus(dto.getStatus());
            userEntity.setUserName(dto.getUserName());
            userEntity.setLastName(dto.getLastName());
            userEntity.setFirstName(dto.getFirstName());
            userRepository.save(userEntity);
        }
        else{
            userRepository.save(dto);
        }
    }


    public Set<Chat> findAllChatsByUser(User user){
        return chatRepository.findAllByUsers(userToEntity(user)).stream().map(this::entityToChat).collect(Collectors.toSet());
    }

    @Transactional
    public void removeChat(String chatId) {
        /*Так как у нас двухсторонние отношения, сначала получаем сущность юзера,
         * Далее сущность чата, отвязываем от пользователя чат = далее удаляем его из списка юзера
         * далее можем удалить сам чат, обязательно реализовать HashCode $Equals
         * При присоединении/отсоединении бота нам приходит айди чата и юзер
         * Соответственно, получили юзера, получили чат, далее проверяем*/
            //нашли чат, далее нужно найти всех пользователей этого чата
            Optional<ChatEntity> optionalChatEntity = chatRepository.findByChatId(chatId);
            if (optionalChatEntity.isPresent()) {//получили из репозитория
                ChatEntity chatEntity = optionalChatEntity.get();
                Set<UserEntity> users = Set.copyOf(chatEntity.getUsers());//получили всех пользователей
                for(UserEntity userEntity : users){
                    userEntity.removeChat(chatEntity);//удалили нашего пользователя
                    if(chatEntity.getUsers().isEmpty()){//если у канала нет пользователей
                        Set<PostEntity> posts = Set.copyOf(chatEntity.getPosts());
                        for(PostEntity post: posts){
                            post.removeChat(chatEntity);
                            if(post.getChats().isEmpty()){//если чатов не осталось, то ставим статус Deleted и сохраняем
                                post.setPostState(PostState.DELETED);//FIXME также нужно проверить что удалили таймеры
                                log.info("Пост {} пользователя {} сейчас сохранен в бд со статусом Deleted," +
                                                " без связи с пользователем",
                                        post.getPostId(),userEntity.getUserId());
                                //FIXME postSender.removeTimer()
                                postRepository.save(post);
                            }
                        }
                        chatRepository.delete(chatEntity);
                        log.info("Канал {}: {} для пользователя {} удален из БД", chatId,
                                chatEntity.getTitle(), userEntity.getUserId());
                    }
                }
            }
    }

    public Optional<UserEntity> findUserById(User user){
        return userRepository.findByUserId(user.getId().toString());
    }

    public Map<Long, Integer> findUsersWithChat(String chatId){
        Map<Long,Integer> userChat = new HashMap<>();
        Optional<ChatEntity> byChatId = chatRepository.findByChatId(chatId);
        if(byChatId.isPresent()){
            Set<UserEntity> users = byChatId.get().getUsers();
            for (var user :users){
                userChat.put(Long.valueOf(user.getUserId()),user.getChats().size());
                }
            }
        return userChat;
    }

    @Transactional
    public void removeUser(User user){
        Optional<UserEntity> userOptional = userRepository.findByUserId(user.getId().toString());
        if(userOptional.isPresent()){
            UserEntity userEntity = userOptional.get();
            Set<ChatEntity> chats = Set.copyOf(userEntity.getChats());
            List<PostEntity> posts = List.copyOf(userEntity.getPosts());

            for(ChatEntity chat: chats) {
                userEntity.removeChat(chat);
            }

            for(ChatEntity chat: chats){
                if(chat.getUsers().isEmpty()){
                    chatRepository.delete(chat);}
                }
            for(PostEntity p: posts){
                userEntity.removePost(p);
                postRepository.delete(p);
            }
        }
        userRepository.delete(userToEntity(user));
        log.info("Пользователь {} удален из БД", user.getId());
    }

    public List<UserEntity> findAll(){
        return userRepository.findAll();
    }

    public int getUserMaxPosts(User user) {
        Optional<UserEntity> byUserId = userRepository.findByUserId(user.getId().toString());
        if(byUserId.isPresent()){
            UserStatus status = byUserId.get().getStatus();
            if(status==UserStatus.STANDARD){
                return standard;
            }
            if(status==UserStatus.PREMIUM){
                return premium;
            }
            if(status==UserStatus.UNLIMITED){
                return -1;
            }
        }
        return -1;
    }

////////////////////////////Конвертеры////////////////////////////
    public UserEntity userToEntity(User user){
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName(user.getFirstName());
        userEntity.setLastName(user.getLastName());
        userEntity.setUserId(user.getId().toString());
        userEntity.setUserName(user.getUserName());
        return userEntity;
    }
    public User entityToUser(UserEntity entity){
        User user = new User();
        user.setId(Long.valueOf(entity.getUserId()));
        user.setUserName(entity.getUserName());
        user.setFirstName(entity.getFirstName());
        user.setLastName(entity.getLastName());
        return user;
    }

    private ChatEntity chatToEntity(Chat chat){
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setChatId(chat.getId().toString());
        chatEntity.setTitle(chat.getTitle());
        return chatEntity;
    }

    public Chat entityToChat(ChatEntity chatEntity){
        Chat chat = new Chat();
        chat.setId(Long.valueOf(chatEntity.getChatId()));
        chat.setTitle(chatEntity.getTitle());
        return chat;
    }
}
