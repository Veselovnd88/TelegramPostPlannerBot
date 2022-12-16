package ru.veselov.plannerBot.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostCreator;
import ru.veselov.plannerBot.service.PostService;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class UserDataCache implements DataCache {

    private final PostService postService;

    private final Map<Long, PostCreator> postCreators = new HashMap<>();
    private final Map<Long, BotState> botStates = new HashMap<>();

    private final Map<Long,Integer> postForManage = new HashMap<>();

    @Autowired
    public UserDataCache(PostService postService) {
        this.postService = postService;
    }

    @Override
    public void setUserBotState(Long userId,BotState botState) {
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
    public PostCreator createPostCreator(User user) {
        PostCreator postCreator = new PostCreator(user);
        log.info("Создается новый PostCreator для {}",user.getId());
        postCreators.put(user.getId(), postCreator);
        return postCreator;
    }

    @Override
    public PostCreator getPostCreator(Long userId) {
        return postCreators.get(userId);
    }

    @Override
    public Map<Long, PostCreator> getPostCreators() {
        return postCreators;
    }

    @Override
    public void removePostCreator(Long userId) {
        postCreators.remove(userId);
    }

    @Override
    public void saveToRepository(Long userId) {
        Post post = postCreators.get(userId).getPost();
        post.setPostState(PostState.CREATED);
        postService.planPost(post);
        setUserBotState(userId,BotState.READY_TO_WORK);
        log.info("Установлен статус бота READY_TO_WORK для {}", userId);
        removePostCreator(userId);
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
}
