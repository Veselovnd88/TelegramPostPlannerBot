package ru.veselov.plannerBot.cache;

import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.service.PostCreator;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public interface DataCache {
    void setUserBotState(Long userId,BotState botState);
    BotState getUsersBotState(Long userId);
    PostCreator createPostCreator(User user);
    PostCreator getPostCreator(Long userId);
    Map<Long,PostCreator> getPostCreators();
    void removePostCreator(Long userId);
    void saveToRepository(Long userId);

    void addPostForManage(Long userId, Integer num);
    void removePostForManage(Long userId);
    Integer getPostForManage(Long userId);

    public Map<Long, Calendar> getSavedDate();
    public Map<Long, Date> getStartedDate();


}
