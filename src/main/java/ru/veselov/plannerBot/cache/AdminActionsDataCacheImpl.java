package ru.veselov.plannerBot.cache;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.controller.BotState;

import java.util.HashMap;
import java.util.Map;

@Component
public class AdminActionsDataCacheImpl implements AdminActionsDataCache {
    private final Map<Long,BotState> startBotState = new HashMap<>();
    private User user;

    @Override
    public void setStartBotState(Long userId, BotState botState) {
        startBotState.put(userId,botState);
    }

    @Override
    public BotState getStartBotState(Long userId) {
        startBotState.get(userId);
    }

    @Override
    public User getPromoteUser() {
        return user;
    }

    @Override
    public void setPromoteUser(User user) {
        this.user=user;
    }
}
