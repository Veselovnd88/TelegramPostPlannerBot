package ru.veselov.plannerBot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.handlers.*;
import ru.veselov.plannerBot.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class UpdateController {

    private final MyPreciousBot bot;
    private final CreatePostHandler createPostHandler;
    private final AddDataHandler addDataHandler;
    private final CallBackQueriesHandler callBackQueriesHandler;
    private final CommandMenuHandler commandMenuHandler;
    private final ManageHandler manageHandler;
    private final DataCache userDataCache;

    private final UserService userService;


    @Autowired
    public UpdateController(CreatePostHandler createPostHandler, MyPreciousBot myPreciousBot,
                            AddDataHandler addDataHandler, CallBackQueriesHandler callBackQueriesHandler, CommandMenuHandler commandMenuHandler,
                            ManageHandler manageHandler, DataCache userDataCache, UserService userService) {
        this.createPostHandler = createPostHandler;
        this.addDataHandler = addDataHandler;
        this.callBackQueriesHandler = callBackQueriesHandler;
        this.commandMenuHandler = commandMenuHandler;
        this.bot = myPreciousBot;
        this.manageHandler = manageHandler;
        this.userDataCache = userDataCache;
        this.userService = userService;
    }

    public void processUpdate(Update update){
       //Проверка присоединения к каналу
        if(update.hasMyChatMember()){
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
                        userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                        log.info("Бот добавлен в канал {} пользователя {}", chat.getTitle(), userId);
                        bot.sendMessageBot(new SendMessage(userId.toString(),
                                "Вы присоединили меня к каналу "+chat.getTitle()));
                    }
                    //Если статус был left - то удаляем чат из списка, проверяем если список пустой,
                    //изменяем состояние бота
                    if(update.getMyChatMember().getNewChatMember().getStatus()
                            .equalsIgnoreCase("left")
                    ||
                            update.getMyChatMember().getNewChatMember().getStatus()
                                    .equalsIgnoreCase("kicked")){
                        /*Если сразу удалять бота - MyChatMember - отдает id пользователя,
                        * если через некоторое время, id возвращается id бота*/
                        Map<Long,Integer> ids = userService.findUsersWithChat(chat.getId().toString());
                        for(var pair : ids.entrySet() ){
                            if(pair.getValue()==1){
                                userDataCache.setUserBotState(pair.getKey(), BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL);
                                log.info("Статус переключен на Ожидание канала для {}",
                                    pair.getKey());
                            }
                            if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("left")){
                                userService.removeChat(chat.getId().toString());
                                bot.sendMessageBot(new SendMessage(pair.getKey().toString(),
                                        "Я больше не админ канала "+chat.getTitle()));
                            }
                            if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("kicked")){
                                userService.removeChat(chat.getId().toString());
                                bot.sendMessageBot(new SendMessage(pair.getKey().toString(),
                                        "Я кикнут с канала "+chat.getTitle()+"чтобы продолжить работу удалите меня из администраторов, и присоедините снова"));
                            }
                        }
                    }
                    return;
                }
            } catch (TelegramApiException e) {
                log.error("Ошибка при присоединении бота к каналу: {}", e.getMessage());
            }
        }

        /*Блок в который попадают апдейты, которые не содержат команды при включенных флагах ожидания поста и даты*/
        if(update.hasMessage()){
            Long userId = update.getMessage().getFrom().getId();
            if(userDataCache.getUsersBotState(userId)==BotState.AWAITING_POST){
                    if((update.getMessage().hasText()||update.getMessage().hasPhoto()
                    || update.getMessage().hasAudio()
                    ||update.getMessage().hasVideo()
                    ||update.getMessage().hasDocument()
                    ||update.getMessage().hasPoll())&&!isCommand(update.getMessage().getText())) {
                        bot.sendMessageBot(createPostHandler.processUpdate(update));
                        }
                    }
                if(userDataCache.getUsersBotState(userId)==BotState.AWAITING_DATE){
                    if((!isCommand(update.getMessage().getText()))) {
                        bot.sendMessageBot(addDataHandler.processUpdate(update));
                    }
                }
        }
        //Апдейты с текстами
        if(update.hasMessage()&& update.getMessage().hasText()){
            if(isCommand(update.getMessage().getText())){
                bot.sendMessageBot(commandMenuHandler.processUpdate(update));
            }
            if(userDataCache.getUsersBotState(update.getMessage().getFrom().getId())==BotState.MANAGE){
                bot.sendMessageBot(manageHandler.processUpdate(update));
            }
        }
        //Обработка запросов с Коллбэками
        if(update.hasCallbackQuery()){
           bot.sendMessageBot(callBackQueriesHandler.processUpdate(update));
        }
    }

    private boolean isCommand(String string){
        List<String> commands = new ArrayList<>(List.of("/start", "/view", "/reset", "/help", "/create"));
        return commands.contains(string);
    }

}
