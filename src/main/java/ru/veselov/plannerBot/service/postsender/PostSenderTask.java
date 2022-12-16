package ru.veselov.plannerBot.service.postsender;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;

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
        this.bot = bot;
        this.post = post;
        this.postService = postService;
        this.postSender = postSender;
    }

    @Override
    public void run() {
        postSender.send(post);
        post.setPostState(PostState.SENT);
        postService.savePost(post);
    }
}
