package ru.veselov.plannerBot.service;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
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
    public void addText(String text) {
        log.info("Добавлен текст к посту пользователя {}", user.getId());
        post.getTexts().add(text);
    }


    public void addPhoto(PhotoSize photo) {
        log.info("Добавлены картинки к посту пользователя {}",user.getId());
        this.post.getPhotos().add(photo);
    }
    public void addCaption(String fileId, String caption){
        log.info("Добавлен заголовок к картинке к посту пользователя {}",user.getId());
        this.post.getCaption().put(fileId,caption);
    }

    public void addMessage(Message message){
        log.info("Добавлено сообщение из телеграм {}",user.getId());
        this.post.getMessages().add(message);
    }



    public void addAudio(Audio audio) {
        log.info("Добавлен аудиотрек к посту пользователя {}",user.getId());
        this.post.getAudios().add(audio);
    }

    public void addDocument(Document document) {
        log.info("Добавлен документ к посту пользователя {}",user.getId());
        this.post.getDocs().add(document);
    }

    public void addVideo(Video video) {
        log.info("Добавлено видео к посту пользователя {}", user.getId());
        this.post.getVideos().add(video);
    }

    public void addPoll(Poll poll){
        log.info("Добавлен опрос к посту пользователя {}", user.getId());
        this.post.getPolls().add(poll);
    }
}
