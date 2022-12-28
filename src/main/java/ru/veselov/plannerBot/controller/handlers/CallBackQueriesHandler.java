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
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.controller.UpdateHandler;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class CallBackQueriesHandler implements UpdateHandler {

    private final DataCache userDataCache;
    private final UserService userService;
    private final PostService postService;
    private final PromoteUserCallbackHandler promoteUserCallback;

    private final ManagePostCallbackQueryHandler managePostCallbackQueryHandler;
    private final ChooseDateHandler chooseDateHandler;
    @Autowired
    public CallBackQueriesHandler(DataCache userDataCache, UserService userService, PostService postService, PromoteUserCallbackHandler promoteUserCallback, ManagePostCallbackQueryHandler managePostCallbackQueryHandler, ChooseDateHandler chooseDateHandler) {
        this.userDataCache = userDataCache;
        this.userService = userService;
        this.postService = postService;
        this.promoteUserCallback = promoteUserCallback;
        this.managePostCallbackQueryHandler = managePostCallbackQueryHandler;
        this.chooseDateHandler = chooseDateHandler;
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
                Post processedPost = userDataCache.getPostCreator(userId).getPost();
                if(names.contains(data)) {
                    for (Chat chat : chats) {
                        if (chat.getTitle().equals(data)) {
                            processedPost.getChats().add(chat);
                        }
                            }
                            log.info("Посты пользователя {} сохранены для публикации в каналах {}",userId,
                                    chats.stream().
                                    map(Chat::getTitle).map(MessageUtils::shortenString).toList());
                            return chooseDateHandler.processUpdate(update);
                    }
                else if(data.equals("postAll")){
                        for(Chat chat: chats){
                            processedPost.getChats().add(chat);
                    }
                            log.info("Посты сохранены для публикации в каналах {}", chats.stream().
                                    map(Chat::getTitle).map(MessageUtils::shortenString).toList());
                            return chooseDateHandler.processUpdate(update);
                    }
                return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                        .text(MessageUtils.INLINE_BUTTON_WITH_UNKNOWN_DATA).showAlert(true).build();

            case AWAITING_DATE:
                return chooseDateHandler.processUpdate(update);

            case READY_TO_SAVE:
                if(data.equals("saveYes")) {
                    userDataCache.saveToRepository(userId);
                    userDataCache.getSavedDate().remove(userId);
                    userDataCache.getStartedDate().remove(userId);
                    log.info("Пост сохранены в базу для пользователя {}",userId);
                    return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                            .text(MessageUtils.POST_SAVED)
                            .showAlert(true).cacheTime(10)
                            .build();
                }
                if(data.equals("inputDate")){
                    userDataCache.setUserBotState(userId,BotState.AWAITING_POST);
                    log.info("Продолжаем выбирать дату");
                    return chooseDateHandler.processUpdate(update);
                }
            case MANAGE:
                return managePostCallbackQueryHandler.processUpdate(update);

            case VIEW:
                if(data.equals("manage")){
                    userDataCache.setUserBotState(userId,BotState.MANAGE);
                    ReplyKeyboardMarkup replyKeyboardMarkup = setKeyboardChosePostId(update.getCallbackQuery().getFrom());
                    return SendMessage.builder().chatId(userId.toString())
                            .text("Введите или выберите из списка ID поста для управления")
                            .replyMarkup(replyKeyboardMarkup).build();
                }

            case PROMOTE_USER:
                return promoteUserCallback.processUpdate(update);
        }
        return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                .text(MessageUtils.DONT_AWAIT_CONTENT).build();

        }

        //////////////////////////////
        private ReplyKeyboardMarkup setKeyboardChosePostId(User user){
            List<Post> allPlanned = postService.findByUserAndPostStates(user,
                    List.of(PostState.SAVED,PostState.PLANNED));
            List<KeyboardRow> keyboardRows = new ArrayList<>();
            KeyboardRow keyboardRow=new KeyboardRow();
            for (int i=1; i<=allPlanned.size(); i++){
                keyboardRow.add(new KeyboardButton(String.valueOf(allPlanned.get(i-1).getPostId())));
                if(i%7==0 || i==(allPlanned.size())){
                    keyboardRows.add(keyboardRow);
                    keyboardRow= new KeyboardRow();
                }
            }
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        return replyKeyboardMarkup;
        }

}

