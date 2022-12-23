package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.postsender.PostSender;
import ru.veselov.plannerBot.utils.MessageUtils;
import ru.veselov.plannerBot.utils.Utils;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class ManagePostCallbackQueryHandler implements UpdateHandler{
    private final PostService postService;
    private final DataCache userDataCache;
    private final PostSender postSender;
    private final Utils utils;

    public ManagePostCallbackQueryHandler(PostService postService, DataCache userDataCache, PostSender postSender, Utils utils) {
        this.postService = postService;
        this.userDataCache = userDataCache;
        this.postSender = postSender;
        this.utils = utils;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        String data = update.getCallbackQuery().getData();
        Long userId = update.getCallbackQuery().getFrom().getId();
        //удаление поста
        if(data.equals("delete")){
            Integer postForManage = userDataCache.getPostForManage(userId);
            postService.deleteById(postForManage);
            SendMessage message = new SendMessage(userId.toString(), MessageUtils.DELETED);
            userDataCache.setUserBotState(userId, BotState.READY_TO_WORK);
            log.info("Пользователь {} удалил пост {}", userId,postForManage);
            userDataCache.removePostForManage(userId);
            return utils.removeKeyBoard(message);
        }
        //просмотр поста
        if(data.equals("view")){
            Optional<Post> post = postService.findById(userDataCache.getPostForManage(userId));
            if(post.isPresent()){
                Chat selfChat = new Chat();
                selfChat.setTitle("Пользователь "+userId);
                selfChat.setId(userId);
                post.get().setChats(Set.of(selfChat));
                postSender.send(post.get());
                SendMessage message = new SendMessage(userId.toString(),MessageUtils.SHOW);
                userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                return utils.removeKeyBoard(message);
            }
            else{
                userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                return new SendMessage(userId.toString(),"Пост не найден");
            }
        }
        //отправить прямо сейчас
        if(data.equals("send")){
            Optional<Post> post = postService.findById(userDataCache.getPostForManage(userId));
            if(post.isPresent()){
                Post postToSend = post.get();
                postToSend.setDate(new Date());
                postService.planPost(postToSend);
                SendMessage message = new SendMessage(userId.toString(), MessageUtils.POST_SENT);
                userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                return utils.removeKeyBoard(message);
            }
        }
        else{
            userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
            return utils.removeKeyBoard(new SendMessage(userId.toString(),"Пост не найден"));
        }
        AnswerCallbackQuery botIsBusyMessage = new AnswerCallbackQuery();//FIXME вынести в Utils
        botIsBusyMessage.setCallbackQueryId(update.getCallbackQuery().getId());
        botIsBusyMessage.setText(MessageUtils.DONT_AWAIT_CONTENT);
        return botIsBusyMessage;
    }
}
