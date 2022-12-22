package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.MessageUtils;
import ru.veselov.plannerBot.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ru.veselov.plannerBot.utils.MessageUtils.*;

@Component
@Slf4j
public class CommandMenuHandler implements UpdateHandler {
    private final Utils utils;
    private final DataCache userDataCache;
    private final PostService postService;
    @Autowired
    public CommandMenuHandler(Utils utils, DataCache userDataCache, PostService postService, UserService userService) {
        this.utils = utils;
        this.userDataCache = userDataCache;
        this.postService = postService;
        this.userService = userService;
    }
    @Autowired
    public final UserService userService;
    @Override
    public SendMessage processUpdate(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        User user = update.getMessage().getFrom();
        String receivedMessage = update.getMessage().getText();

        switch (receivedMessage){
/*При нажатии на стар - проверяется текущий статус, если бот не ожидает добавления в канал,
* то пост сбрасывается (в методе reset также проверяется наличие каналов и устанавливается
*  статус бота - ready - если есть каналы, и bot waiting если нет каналов */
            case "/start":
                log.info("Нажата команда /start");
                if(userDataCache.getUsersBotState(userId)!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                    reset(update);
                }
                return utils.removeKeyBoard(greetings(update));
/*При нажатии на создание поста проверяется количество запланированных постов и статус пользователя,
*Если не превышает - проверяем, присоединен ли бот к каналам, и если да, то переводим состояние в
* Ожидание поста */
            case "/create":
                log.info("Нажата команда /create");
                List<Post> byUser = postService.findByUserAndPostStates(user,
                        List.of(PostState.SAVED,PostState.PLANNED));
                int max = userService.getUserMaxPosts(user);
                if(max!=-1 && byUser.size()>=max){
                    return new SendMessage(userId.toString(),POST_LIMIT+max);
                }
                if(userDataCache.getUsersBotState(userId)==BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                    return new SendMessage(update.getMessage().getChatId().toString(),
                            BOT_WAS_NOT_ADDED_TO_CHANEL);}
                userDataCache.createPostCreator(update.getMessage().getFrom());
                userDataCache.setUserBotState(userId,BotState.AWAITING_POST);
                log.info("Бот в состоянии AWAITING_POST для пользователя {}",userId);
                return utils.removeKeyBoard(new SendMessage(update.getMessage().getChatId().toString(),
                        AWAIT_CONTENT_MESSAGE));
            case "/view":
                log.info("Нажата команда /view");
                if(userDataCache.getUsersBotState(userId)!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                    reset(update);
                }
                userDataCache.setUserBotState(userId,BotState.VIEW);
                return viewModeMessage(update);

            case "/reset":
                log.info("Нажата кнопка /reset");
                return utils.removeKeyBoard(reset(update));

            case "/help":
                reset(update);
                log.info("Нажата кнопка /help");
                return utils.removeKeyBoard(new SendMessage(String.valueOf(update.getMessage().getChatId()),
                        HELP_MESSAGE));
        }
        return utils.removeKeyBoard(new SendMessage(String.valueOf(update.getMessage().getChatId()), MessageUtils.UNKNOWN_COMMAND));
    }

    private SendMessage greetings(Update update){
        Set<Chat> allChatsByUser = userService.findAllChatsByUser(update.getMessage().getFrom());
        Set<String> chatNames = allChatsByUser.stream().map(Chat::getTitle).collect(Collectors.toSet());
        //Проверяется, есть ли у пользователя чаты в БД, если есть
        if(allChatsByUser.isEmpty()){
            return new SendMessage(update.getMessage().getChatId().toString(),
                    START_MESSAGE);
        }
        userDataCache.setUserBotState(update.getMessage().getFrom().getId(),BotState.READY_TO_WORK);
        log.info("Бот в состоянии READY_TO_WORK для пользователя {}",update.getMessage().getFrom().getId());
        return new SendMessage(update.getMessage().getChatId().toString(),
                START_MESSAGE_USER_ALREADY_USE_BOT+chatNames);

        }

    /*Удаляет посты, которые в процессе добавления*/
    private SendMessage reset(Update update){
        Long userId = update.getMessage().getFrom().getId();
        userDataCache.removePostCreator(userId);
        BotState botState = userDataCache.getUsersBotState(userId);
        if(botState!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
            userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
        }
        return new SendMessage(update.getMessage().getChatId().toString(),RESET_POSTS);
    }

    private SendMessage viewModeMessage(Update update){
        List<Post> allPlanned = postService.findByUserAndPostStates(update.getMessage().getFrom(),
                List.of(PostState.SAVED,PostState.PLANNED));
        if(allPlanned.isEmpty()){
            return new SendMessage(update.getMessage().getChatId().toString(),NO_PLANNED_POSTS);
        }
        StringBuilder sb = new StringBuilder("Запланированные посты\n");
        for(Post p: allPlanned){
            String format = String.format("Пост № %d, сообщений %s, картинок %s, файлов %s, опросов %s, аудио %s, видео %s," +
                            " будет отправлен %s в канал(ы) %s",
                    p.getPostId(), p.getTexts().size(), p.getPhotos().size(), p.getDocs().size(),
                    p.getPolls().size(), p.getAudios().size(), p.getVideos().size(),
                    p.getDate(), p.getChats().stream().map(Chat::getTitle).toList());
            sb.append(format);
            sb.append("\n");
        }
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton manageButton = new InlineKeyboardButton();
        manageButton.setCallbackData("manage");
        manageButton.setText("Просмотр/удаление поста");
        row1.add(manageButton);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowList);
        message.setReplyMarkup(inlineKeyboardMarkup);
        message.setChatId(update.getMessage().getChatId().toString());
        message.setText(sb.toString());
        return message;
    }
}
