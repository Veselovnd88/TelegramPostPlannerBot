package ru.veselov.plannerBot.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class UserDataCache implements DataCache {
    private final PostService postService;

    private final Map<Long, Post> postCache = new HashMap<>();
    private final Map<Long, BotState> botStates = new HashMap<>();

    private final Map<Long,Integer> postForManage = new HashMap<>();

    public Map<Long, Calendar> getSavedDate() {
        return savedDate;
    }

    public Map<Long, Date> getStartedDate() {
        return startedDate;
    }

    private final Map<Long, Calendar> savedDate = new HashMap<>();
    private final Map<Long, Date> startedDate = new HashMap<>();

    @Autowired
    public UserDataCache(PostService postService) {
        this.postService = postService;
    }

    @Override
    public void setUserBotState(Long userId,BotState botState) {
        log.info("Бот в состоянии {}", botState);
        botStates.put(userId,botState);
    }

    @Override
    public BotState getUsersBotState(Long userId) {
        BotState botState = botStates.get(userId);
        if(botState==null){
            return BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL;
        }
        else return botState;
    }

    @Override
    public Post createPost(User user){
        log.info("Создан и добавлен в кеш пост для {}", user.getId());
        Post post = new Post(user);
        postCache.put(user.getId(), post);
        return post;
    }
    @Override
    public Post getPost(Long userId){
        return postCache.get(userId);
    }
    @Override
    public Map<Long, Post> getPostCache(){
        return postCache;
    }
    @Override
    public void saveToRepository(Long userId) {
        Post post = postCache.get(userId);
        post.setPostState(PostState.CREATED);
        postService.planPost(post);
        setUserBotState(userId,BotState.READY_TO_WORK);
        clear(userId);
    }

    public void addPostForManage(Long userId, Integer num){
        postForManage.put(userId,num);
    }
    public void removePostForManage(Long userId){
        postForManage.remove(userId);
    }
    public Integer getPostForManage(Long userId){
        return postForManage.get(userId);
    }

    @Override
    public void clear(Long id) {
        savedDate.remove(id);
        startedDate.remove(id);
        postCache.remove(id);
        postForManage.remove(id);
    }
}
