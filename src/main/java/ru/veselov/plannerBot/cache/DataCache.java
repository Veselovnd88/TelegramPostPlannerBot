package ru.veselov.plannerBot.cache;

import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.service.PostCreator;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public interface DataCache extends Cache {
    void setUserBotState(Long userId,BotState botState);
    BotState getUsersBotState(Long userId);
    Post createPost(User user);
    Post getPost(Long userId);
    Map<Long,Post> getPostCache();
    void saveToRepository(Long userId);

    void addPostForManage(Long userId, Integer num);
    void removePostForManage(Long userId);
    Integer getPostForManage(Long userId);

    Map<Long, Calendar> getSavedDate();
    Map<Long, Date> getStartedDate();

}
