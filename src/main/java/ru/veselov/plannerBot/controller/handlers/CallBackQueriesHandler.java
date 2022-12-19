package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.service.postsender.PostSender;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.*;

@Component
@Slf4j
public class CallBackQueriesHandler implements UpdateHandler{
    private final DataCache userDataCache;
    private final UserService userService;
    private final PostService postService;
    private final PostSender postSender;
    @Autowired
    public CallBackQueriesHandler(DataCache userDataCache, UserService userService, PostService postService, PostSender postSender) {
        this.userDataCache = userDataCache;
        this.userService = userService;
        this.postService = postService;
        this.postSender = postSender;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        String data = update.getCallbackQuery().getData();
        /*Установка ID чатов для всех постов юзера, если флажок ожидания постов активен -
         * срабатывают, если нет, то при нажатии ничего не будет происходить*/
        Long userId = update.getCallbackQuery().getFrom().getId();
        BotState botState = userDataCache.getUsersBotState(userId);
        switch (botState){
            case AWAITING_POST:
                Set<Chat> chats= userService.findAllChatsByUser(update.getCallbackQuery().getFrom());
                List<String> names = chats.stream()
                        .map(x-> (MessageUtils.shortenString(x.getTitle()))).toList();
                if(names.contains(data)) {
                    for (Chat chat : chats) {
                        if (chat.getTitle().equals(data)) {
                            //Отправляем Пост и чат в сервис - для связки, т.к. тут не транзакционный метод
                            Post post = userDataCache.getPostCreator(userId).getPost();
                            post.getChats().add(chat);
                        }
                            }
                            SendMessage sendPictureMessage =
                                    new SendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                                            MessageUtils.AWAITING_DATE);
                            log.info("Посты пользователя {} сохранены для публикации в каналах {}",userId,
                                    chats.stream().
                                    map(Chat::getTitle).map(MessageUtils::shortenString).toList());
                            userDataCache.setUserBotState(userId,BotState.AWAITING_DATE);
                            return sendPictureMessage;
                    }
                    else if(data.equals("postAll")){
                        Post post = userDataCache.getPostCreator(userId).getPost();
                        for(Chat chat: chats){
                            post.getChats().add(chat);
                    }
                    SendMessage sendPictureMessage =
                                    new SendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                                            MessageUtils.AWAITING_DATE);
                            log.info("Посты сохранены для публикации в каналах {}", chats.stream().
                                    map(Chat::getTitle).map(MessageUtils::shortenString).toList());
                            userDataCache.setUserBotState(userId,BotState.AWAITING_DATE);
                            return sendPictureMessage;
                    }
                    AnswerCallbackQuery unknownAnswer = new AnswerCallbackQuery();
                    unknownAnswer.setCallbackQueryId(update.getCallbackQuery().getId());
                    unknownAnswer.setText(MessageUtils.INLINE_BUTTON_WITH_UNKNOWN_DATA);
                    unknownAnswer.setShowAlert(true);
                    return unknownAnswer;
            case AWAITING_DATE:
                if(data.equals("saveYes")) {
                    userDataCache.saveToRepository(userId);
                    log.info("Пост сохранены в базу для пользователя {}",userId);
                    AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                    answerCallbackQuery.setCallbackQueryId(update.getCallbackQuery().getId());
                    answerCallbackQuery.setText(MessageUtils.POST_SAVED);
                    answerCallbackQuery.setShowAlert(true);
                    answerCallbackQuery.setCacheTime(10);
                    return answerCallbackQuery;
            }
            case MANAGE:
                if(data.equals("delete")){
                    postService.deleteById(userDataCache.getPostForManage(userId));
                    SendMessage message = new SendMessage(userId.toString(),MessageUtils.DELETED);
                    userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                    return message;
                }
                if(data.equals("view")){
                    Optional<Post> post = postService.findById(userDataCache.getPostForManage(userId));
                    if(post.isPresent()){
                        Chat selfChat = new Chat();
                        selfChat.setId(userId);
                        post.get().setChats(Set.of(selfChat));
                        postSender.send(post.get());
                        SendMessage message = new SendMessage(userId.toString(),MessageUtils.SHOW);
                        userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                        return message;
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
                        return message;
                    }
                    else{
                        userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                        return new SendMessage(userId.toString(),"Пост не найден");
                    }
                }

        }
        if(data.equals("manage")){
            userDataCache.setUserBotState(userId,BotState.MANAGE);
            ReplyKeyboardMarkup replyKeyboardMarkup = setKeyboardChosePostId(update.getCallbackQuery().getFrom());

            SendMessage sendMessage = new SendMessage(userId.toString(),
                    "Введите ID поста который вы хотите удалить или просмотреть");//или выберите из списка
            sendMessage.setReplyMarkup(replyKeyboardMarkup);
            return sendMessage;
        }
        AnswerCallbackQuery botIsBusyMessage = new AnswerCallbackQuery();
        botIsBusyMessage.setCallbackQueryId(update.getCallbackQuery().getId());
        botIsBusyMessage.setText(MessageUtils.DONT_AWAIT_CONTENT);
        return botIsBusyMessage;
        }


        private ReplyKeyboardMarkup setKeyboardChosePostId(User user){
            List<Post> allPlanned = postService.findByUserAndPostStates(user,
                    List.of(PostState.SAVED,PostState.PLANNED));
            List<KeyboardRow> keyboardRows = new ArrayList<>();
            KeyboardRow keyboardRow=new KeyboardRow();
            for (int i=0; i<allPlanned.size(); i++){
                if(i!=0 &&i%5==0){
                    keyboardRows.add(keyboardRow);
                    keyboardRow = new KeyboardRow();
                }
                keyboardRow.add(new KeyboardButton(String.valueOf(allPlanned.get(i).getPostId())));
            }
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return replyKeyboardMarkup;

        }
}

