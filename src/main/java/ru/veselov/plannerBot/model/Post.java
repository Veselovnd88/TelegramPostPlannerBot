package ru.veselov.plannerBot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.*;
//DTO
@Getter
@Setter
@NoArgsConstructor
@Slf4j
@EqualsAndHashCode(exclude = "postId")
public class Post {

    private int postId;

    private Date date;

    private PostState postState=PostState.CREATED;

    private Set<Chat> chats= new HashSet<>();

    private List<Message> messages=new LinkedList<>();

    private User user;

    public Post(User user){
        this.user =user;
        this.date = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow")).getTime();
    }

    public void addMessage(Message message){
        log.info("Добавлено сообщение из телеграм {}",user.getId());
        messages.add(message);
    }
}
