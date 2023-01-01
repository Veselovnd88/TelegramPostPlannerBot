package ru.veselov.plannerBot.service.postsender;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.TimerTask;

@Slf4j
@Getter
@Setter
public class PostSenderTask extends TimerTask {
    private final MyPreciousBot bot;
    private final Post post;
    private final PostService postService;
    private final PostSender postSender;
    public PostSenderTask(MyPreciousBot bot, Post post, PostService postService, PostSender postSender){
        //передать только ID поста
        this.bot = bot;
        this.post = post;
        this.postService = postService;
        this.postSender = postSender;
    }

    @Override
    public void run() {
        try {
            postSender.send(post);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение {}", e.getMessage());
            try{
                bot.execute(SendMessage.builder().chatId(post.getUser().getId())
                        .text(MessageUtils.ERROR_MESSAGE).build());
            } catch (TelegramApiException ex) {
                log.error("Не удалось отправить сообщение об ошибке пользователю {}", post.getUser().getId());
            }
        }
         post.setPostState(PostState.SENT);
         postService.savePost(post);
         postSender.removeTimer(post.getPostId());
    }
}
