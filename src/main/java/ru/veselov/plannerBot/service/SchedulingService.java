package ru.veselov.plannerBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.cache.UserDataCache;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Service
public class SchedulingService {
    private final PostService postService;
    private final DataCache userDataCache;
    private final TimeZone tz = TimeZone.getTimeZone("Europe/Moscow");


    @Autowired
    public SchedulingService(PostService postService, UserDataCache userDataCache) {
        this.postService = postService;
        this.userDataCache = userDataCache;
    }
    @PostConstruct
    public void checkPlanned(){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(tz);
        log.info("Проверяю посты со статусом PLANNED");
        //Проверяются все посты со статусом PLANNED, которые не были отправлены
        List<Post> planned = postService.getPostsByState(PostState.PLANNED);
        for(Post post:planned){
            postService.planPost(post);
        }
    }

    @Scheduled(cron = "${bot.period}",zone = "Europe/Moscow")
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
        Map<Long, Post> postCache = userDataCache.getPostCache();
        List<Long> toRemove = new ArrayList<>();
            for(var pair: postCache.entrySet()){
                if(pair.getValue().getDate().before(calendar.getTime())){
                    toRemove.add(pair.getKey());
                }
            }
        toRemove.forEach(postCache::remove);
        toRemove.forEach(x->
                userDataCache.setUserBotState(x,BotState.READY_TO_WORK));
    }
}
