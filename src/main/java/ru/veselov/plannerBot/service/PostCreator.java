package ru.veselov.plannerBot.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.model.Post;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@Getter
@Setter
public class PostCreator {
    private final User user;
    private Date creationDate;
    private Post post = new Post();
    @Autowired
    public PostCreator(User user){
        this.user=user;
        this.creationDate = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).getTime();
        post.setUser(user);
    }

    public void addMessage(Message message){
        log.info("Добавлено сообщение из телеграм {}",user.getId());
        this.post.getMessages().add(message);
    }

}
