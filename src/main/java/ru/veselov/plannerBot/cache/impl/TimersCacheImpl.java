package ru.veselov.plannerBot.cache.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.veselov.plannerBot.cache.TimersCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
@Component
@Slf4j
/*Здесь хранятся таймеры для отправки постов*/
public class TimersCacheImpl implements TimersCache {
    private final Map<Integer, Timer> timers=new HashMap<>();

    @Override
    public Timer getTimer(Integer postId) {
        return timers.get(postId);
    }

    @Override
    public void removeTimer(Integer postId) {
        log.info("Таймер поста {} удален", postId);
        Timer removed = timers.remove(postId);
        removed.purge();
        removed.cancel();
    }

    @Override
    public void addTimer(Integer postId, Timer timer) {
        log.info("Таймер поста {} добавлен", postId);
        timers.put(postId,timer);
    }

    @Override
    public Boolean contains(Integer postId) {
        return timers.containsKey(postId);
    }
}
