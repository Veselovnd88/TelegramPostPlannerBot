package ru.veselov.plannerBot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.handlers.*;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.MessageUtils;
import ru.veselov.plannerBot.utils.Utils;

import java.util.Optional;

@Component
@Slf4j
public class UpdateController implements UpdateHandler {

    private final MyPreciousBot bot;
    private final CreatePostHandler createPostHandler;
    private final ChooseDateHandler chooseDateHandler;
    private final CallBackQueriesHandler callBackQueriesHandler;
    private final CommandMenuHandler commandMenuHandler;
    private final ManagePostTextHandler managePostTextHandler;
    private final PromoteUserTextHandler promoteUserTextHandler;
    private final BotChatMemberActionsHandler botChatMemberActionsHandler;
    private final DataCache userDataCache;

    private final UserService userService;
    private final Utils utils;


    @Autowired
    public UpdateController(CreatePostHandler createPostHandler, MyPreciousBot myPreciousBot,
                            ChooseDateHandler chooseDateHandler, CallBackQueriesHandler callBackQueriesHandler, CommandMenuHandler commandMenuHandler,
                            ManagePostTextHandler managePostTextHandler, PromoteUserTextHandler promoteUserTextHandler, BotChatMemberActionsHandler botChatMemberActionsHandler, DataCache userDataCache, UserService userService, Utils utils) {
        this.createPostHandler = createPostHandler;
        this.chooseDateHandler = chooseDateHandler;
        this.callBackQueriesHandler = callBackQueriesHandler;
        this.commandMenuHandler = commandMenuHandler;
        this.bot = myPreciousBot;
        this.managePostTextHandler = managePostTextHandler;
        this.promoteUserTextHandler = promoteUserTextHandler;
        this.botChatMemberActionsHandler = botChatMemberActionsHandler;
        this.userDataCache = userDataCache;
        this.userService = userService;
        this.utils = utils;
    }

    public BotApiMethod<?> processUpdate(Update update){
       //Проверка присоединения к каналу
        if(update.hasMyChatMember()){
           return botChatMemberActionsHandler.processUpdate(update);
        }

        /*Блок в который попадают апдейты, которые не содержат команды при включенных флагах ожидания поста и даты*/
        if(update.hasMessage()){
            Long userId = update.getMessage().getFrom().getId();
            if(userDataCache.getUsersBotState(userId)== BotState.AWAITING_POST){
                    if((update.getMessage().hasText()||update.getMessage().hasPhoto()
                    || update.getMessage().hasAudio()
                    ||update.getMessage().hasVideo()
                    ||update.getMessage().hasDocument()
                    ||update.getMessage().hasPoll())&&!isCommand(update)) {
                        return createPostHandler.processUpdate(update);
                        }
                    }
                if(userDataCache.getUsersBotState(userId)==BotState.AWAITING_DATE){
                    if(!isCommand(update)){
                        return chooseDateHandler.processUpdate(update);
                    }
                }
        }

        //Жмет старт - получает статус не добавлен к каналу
        //должен получить инструкцию, пока не добавлен к каналу - другие команды не работают
        //при повторном нажатии старт - выдает приветственное сообщение и перечисление каналов (происходжит проверка каналов)
        //Апдейты с текстами
        if(update.hasMessage()&& update.getMessage().hasText()){
            BotState botState = userDataCache.getUsersBotState(update.getMessage().getFrom().getId());
            if(isCommand(update)){
                if(botState!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
                    return commandMenuHandler.processUpdate(update);
                }
                else{
                    if(update.getMessage().getText().equals("/start")
                            ||
                        update.getMessage().getText().equals("/help")
                            ||update.getMessage().getText().equals("/promote")){
                        return commandMenuHandler.processUpdate(update);
                    }
                    else {
                        return SendMessage.builder().chatId(update.getMessage().getChatId().toString())
                                .text(MessageUtils.BOT_WAS_NOT_ADDED_TO_CHANEL).build();
                    }
                }
            }
            else if(botState==BotState.MANAGE){
                return managePostTextHandler.processUpdate(update);
            }
            else if(botState==BotState.PROMOTE_USER){
                return promoteUserTextHandler.processUpdate(update);
            }
        }
        //Обработка коллбэков
        if(update.hasCallbackQuery()){
           BotState botState = userDataCache.getUsersBotState(update.getCallbackQuery().getFrom().getId());
           if(botState!=BotState.BOT_WAITING_FOR_ADDING_TO_CHANNEL){
              return callBackQueriesHandler.processUpdate(update);}
        }
        return null;
    }

    private boolean isCommand(Update update) {
        if (update.hasMessage() && update.getMessage().hasEntities()) {
            Optional<MessageEntity> commandEntity = update.getMessage().getEntities()
                    .stream().filter(x -> "bot_command".equals(x.getType())).findFirst();
            return commandEntity.isPresent();
        }
        return false;
    }

}
