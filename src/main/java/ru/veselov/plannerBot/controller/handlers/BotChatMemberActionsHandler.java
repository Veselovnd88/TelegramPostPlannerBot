package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.controller.UpdateHandler;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.Utils;

import java.util.Map;

@Component
@Slf4j
public class BotChatMemberActionsHandler implements UpdateHandler {
    private final UserService userService;
    private final Utils utils;
    final private DataCache userDataCache;
    private final MyPreciousBot bot;
    @Autowired
    public BotChatMemberActionsHandler(UserService userService, Utils utils, DataCache userDataCache, MyPreciousBot bot) {
        this.userService = userService;
        this.utils = utils;
        this.userDataCache = userDataCache;
        this.bot = bot;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        try {
            if((update.getMyChatMember().getNewChatMember().getUser().getId())//бота присоединили к каналу
                    .equals(bot.getMe().getId())){
                User user = update.getMyChatMember().getFrom();
                Long userId = user.getId();
                Chat chat = update.getMyChatMember().getChat();
                if(update.getMyChatMember().getNewChatMember().getStatus()
                        .equalsIgnoreCase("administrator")){
                    //Сохраняем в БД пользователя с назначенным чатом
                    userService.save(chat,user);
                    userDataCache.setUserBotState(userId, BotState.READY_TO_WORK);
                    log.info("Бот добавлен в канал {} пользователя {}", chat.getTitle(), userId);
                    return utils.removeKeyBoard(SendMessage.builder().chatId(userId)
                            .text("Вы присоединили меня к каналу "+chat.getTitle()).build());
                }
                //Если статус был left - то удаляем чат из списка, проверяем если список пустой,
                //изменяем состояние бота
                if(update.getMyChatMember().getNewChatMember().getStatus()
                        .equalsIgnoreCase("left")
                        ||
                        update.getMyChatMember().getNewChatMember().getStatus()
                                .equalsIgnoreCase("kicked")){
                    Map<Long,Integer> ids = userService.findUsersWithChat(chat.getId().toString());
                    for(var pair : ids.entrySet() ){
                        if(pair.getValue()==1){
                            userDataCache.setUserBotState(pair.getKey(), BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
                            log.info("Статус переключен на Ожидание канала для {}",
                                    pair.getKey());
                        }
                        if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("left")){
                            userService.removeChat(chat.getId().toString());
                            return utils.removeKeyBoard(SendMessage.builder().chatId(pair.getKey())
                                    .text("Я больше не админ канала "+chat.getTitle()).build());
                        }
                        if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("kicked")){
                            userService.removeChat(chat.getId().toString());
                            return utils.removeKeyBoard(SendMessage.builder().chatId(pair.getKey())
                                    .text(
                                            "Я кикнут с канала "+chat.getTitle()+"чтобы продолжить работу удалите меня из администраторов, и присоедините снова").build());
                        }
                    }
                }
                return null ;
            }
        } catch (TelegramApiException e) {
            log.error("Ошибка при присоединении бота к каналу: {}", e.getMessage());
        }
        return null;
    }
}
