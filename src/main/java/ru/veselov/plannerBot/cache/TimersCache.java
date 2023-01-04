package ru.veselov.plannerBot.cache;

import java.util.Timer;

public interface TimersCache{
    Timer getTimer(Integer postId);
    void removeTimer(Integer postId);
    void addTimer(Integer postId, Timer timer);
    Boolean contains(Integer postId);
}
