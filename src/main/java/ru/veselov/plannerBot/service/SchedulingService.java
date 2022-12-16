package ru.veselov.plannerBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.cache.UserDataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;

import java.util.*;

//@Service
@Slf4j
@Service
public class SchedulingService {
    private final MyPreciousBot bot;
    private final PostService postService;
    private final DataCache userDataCache;
    private final TimeZone tz = TimeZone.getTimeZone("Europe/Moscow");

    @Autowired
    public SchedulingService(MyPreciousBot bot, PostService postService, UserDataCache userDataCache) {
        this.bot = bot;
        this.postService = postService;
        this.userDataCache = userDataCache;
    }

    @Scheduled(cron = "0 0 0 * * *",zone = "Europe/Moscow")
    public void scheduledTask(){
        checkPostCreators();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(tz);
        log.info("Проверяю хранилище постов и назначаю в очереди в {}", calendar.getTime());
        calendar.add(Calendar.DATE, 1);

        List<Post> today = postService.todayPosts(calendar.getTime(), PostState.SAVED);
            for(Post post:today){
                postService.planPost(post);
            }
        }
    /*Проверка на висящие объекты - если юзер начал создавать пост и не закончил, то обнуляем
    * пост и ставим статус бота в готов к работе*/
    public void checkPostCreators(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(tz);
        calendar.add(Calendar.DATE,-1);
        Map<Long, PostCreator> postCreators = userDataCache.getPostCreators();
        List<Long> toRemove = new ArrayList<>();
            for(var pair: postCreators.entrySet()){
                if(pair.getValue().getCreationDate().before(calendar.getTime())){
                    toRemove.add(pair.getKey());
                }
            }
        toRemove.forEach(postCreators::remove);
        toRemove.forEach(x->
                userDataCache.setUserBotState(x,BotState.READY_TO_WORK));
    }
}
