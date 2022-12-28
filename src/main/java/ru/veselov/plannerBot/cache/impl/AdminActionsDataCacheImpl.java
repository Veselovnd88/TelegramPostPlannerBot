package ru.veselov.plannerBot.cache.impl;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.cache.AdminActionsDataCache;
import ru.veselov.plannerBot.bots.BotState;

import java.util.HashMap;
import java.util.Map;

@Component
public class AdminActionsDataCacheImpl implements AdminActionsDataCache {
    private final Map<Long,BotState> startBotState = new HashMap<>();
    private final Map<Long,User> promoteUser = new HashMap<>();

    @Override
    public void setStartBotState(Long userId, BotState botState) {
        startBotState.put(userId,botState);
    }

    @Override
    public BotState getStartBotState(Long userId) {
        return startBotState.get(userId);
    }

    @Override
    public User getPromoteUser(Long userId) {
        return promoteUser.get(userId);
    }

    @Override
    public void setPromoteUser(Long userId, User user) {
        promoteUser.put(userId,user);
    }
}
