package ru.veselov.plannerBot.cache;

import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.controller.BotState;

public interface AdminActionsDataCache {
    void setStartBotState(Long userId,BotState botState);
    BotState getStartBotState(Long userId);
    User getPromoteUser(Long userId);
    void setPromoteUser(Long userId, User user);
}
