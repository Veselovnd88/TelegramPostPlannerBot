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
        log.info("Нажата команда {}",receivedMessage);
        switch (receivedMessage){
/*При нажатии на старт - проверяется текущий статус, если бот не ожидает добавления в канал,
* то пост сбрасывается (в методе reset также проверяется наличие каналов и устанавливается
*  статус бота - ready - если есть каналы, и bot waiting если нет каналов */
            case "/start":
                if(userDataCache.getUsersBotState(userId)!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                    reset(update);
                }
                else{
                    userService.saveUser(userService.userToEntity(user));
                    userDataCache.setUserBotState(userId,BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
                }
                return utils.removeKeyBoard(greetings(update));
/*При нажатии на создание поста проверяется количество запланированных постов и статус пользователя,
*Если не превышает - проверяем, присоединен ли бот к каналам, и если да, то переводим состояние в
* Ожидание поста */
            case "/create":
                if(BotState.READY_TO_WORK==userDataCache.getUsersBotState(userId)){
                    List<Post> byUser = postService.findByUserAndPostStates(user,
                            List.of(PostState.SAVED,PostState.PLANNED));
                    int max = userService.getUserMaxPosts(user);
                    if(max!=-1 && byUser.size()>=max){
                        return new SendMessage(userId.toString(),POST_LIMIT+max);
                    }
                    userDataCache.createPostCreator(update.getMessage().getFrom());
                    userDataCache.setUserBotState(userId,BotState.AWAITING_POST);
                    return new SendMessage(update.getMessage().getChatId().toString(),
                            AWAIT_CONTENT_MESSAGE);}
                else{
                   return SendMessage.builder().chatId(userId)
                            .text(ANOTHER_ACTION_IN_PROCESS).build();
                }
            case "/view":
                if(userDataCache.getUsersBotState(userId)==BotState.READY_TO_WORK){
                    userDataCache.setUserBotState(userId,BotState.VIEW);
                    return viewModeMessage(update);
                }
                else{
                    return SendMessage.builder().chatId(userId)
                            .text(ANOTHER_ACTION_IN_PROCESS).build();
                }

            case "/reset":
                return utils.removeKeyBoard(reset(update));

            case "/help":
                reset(update);
                return utils.removeKeyBoard(new SendMessage(String.valueOf(update.getMessage().getChatId()),
                        HELP_MESSAGE));

            case "/promote":
                 reset(update);
                 //FIXME понять в какой статус возвращать бота после отработки команды Promote
                 userDataCache.setUserBotState(userId,BotState.PROMOTE_USER);
                 return utils.removeKeyBoard(
                         SendMessage.builder().chatId(String.valueOf(update.getMessage().getChatId()))
                                 .text(FORWARD_MESSAGE).build()
                 );
        }
        return utils.removeKeyBoard(SendMessage.builder().chatId(userId).text(UNKNOWN_COMMAND).build());
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
        return new SendMessage(update.getMessage().getChatId().toString(),
                START_MESSAGE_USER_ALREADY_USE_BOT+chatNames);

        }

    private SendMessage reset(Update update){
        Long userId = update.getMessage().getFrom().getId();
        userDataCache.removePostCreator(userId);//FIXME удалить другие кеши
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
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton manageButton = new InlineKeyboardButton();
        manageButton.setCallbackData("manage");
        manageButton.setText("Просмотр/удаление поста");
        row1.add(manageButton);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return SendMessage.builder().replyMarkup(inlineKeyboardMarkup).chatId(update.getMessage().getChatId())
                        .text(sb.toString()).build();
    }
}
