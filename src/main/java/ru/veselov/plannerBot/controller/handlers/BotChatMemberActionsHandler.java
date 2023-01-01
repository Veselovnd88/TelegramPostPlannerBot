package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
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
            if((update.getMyChatMember().getNewChatMember().getUser().getId())//бота присоединили к каналу
                    .equals(bot.getMyId())){
                User user = update.getMyChatMember().getFrom();
                Long userId = user.getId();
                Chat chat = update.getMyChatMember().getChat();
                if(update.getMyChatMember().getNewChatMember().getStatus()
                        .equalsIgnoreCase("administrator")){
                    //Сохраняем в БД пользователя с назначенным чатом
                    userService.save(chat,user);
                    userDataCache.setUserBotState(userId, BotState.READY_TO_WORK);
                    log.info("Бот добавлен в канал {} пользователя {}", chat.getTitle(), userId);
                    userDataCache.clear(userId);
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
                    //Пишет каждому пользователю, что бота удалили с канала
                    for(var pair : ids.entrySet() ){
                        userDataCache.clear(pair.getKey());
                        if(pair.getValue()==1){
                            userDataCache.setUserBotState(pair.getKey(), BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
                            log.info("Статус переключен на Ожидание канала для {}",
                                    pair.getKey());
                        }
                        else{
                            userDataCache.setUserBotState(pair.getKey(),BotState.READY_TO_WORK);
                        }
                        String message="";
                        if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("left")){
                            message=String.format("Я больше не админ канала %s",chat.getTitle());
                        }
                        if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("kicked")){
                            message=String.format("Я кикнут с канала %s,чтобы продолжить работу удалите меня из администраторов, и присоедините снова",chat.getTitle());
                        }
                        userService.removeChat(chat.getId().toString());
                        return utils.removeKeyBoard(SendMessage.builder().chatId(pair.getKey())
                                .text(message).build());
                    }
                }
                log.info("Что то другое произошло со статусом бота {}",update.getMyChatMember());
                return null ;
            }
        log.info("В канале произошло изменение {}",update.getMyChatMember());
        return null;
    }
}