package ru.veselov.plannerBot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.handlers.*;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.Map;
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
    private final DataCache userDataCache;

    private final UserService userService;


    @Autowired
    public UpdateController(CreatePostHandler createPostHandler, MyPreciousBot myPreciousBot,
                            ChooseDateHandler chooseDateHandler, CallBackQueriesHandler callBackQueriesHandler, CommandMenuHandler commandMenuHandler,
                            ManagePostTextHandler managePostTextHandler, PromoteUserTextHandler promoteUserTextHandler, DataCache userDataCache, UserService userService) {
        this.createPostHandler = createPostHandler;
        this.chooseDateHandler = chooseDateHandler;
        this.callBackQueriesHandler = callBackQueriesHandler;
        this.commandMenuHandler = commandMenuHandler;
        this.bot = myPreciousBot;
        this.managePostTextHandler = managePostTextHandler;
        this.promoteUserTextHandler = promoteUserTextHandler;
        this.userDataCache = userDataCache;
        this.userService = userService;
    }

    public BotApiMethod<?> processUpdate(Update update){
       //Проверка присоединения к каналу
        if(update.hasMyChatMember()){
            try {
                if((update.getMyChatMember().getNewChatMember().getUser().getId())//бота присоединили к каналу
                        .equals(bot.getMe().getId())){
                    User user = update.getMyChatMember().getFrom();
                    Long userId = user.getId();//FIXME продумать этот момент, т.к. этот апдейт переводит бота в другой статус незапланировано
                    Chat chat = update.getMyChatMember().getChat();
                    if(update.getMyChatMember().getNewChatMember().getStatus()
                            .equalsIgnoreCase("administrator")){
                        //Сохраняем в БД пользователя с назначенным чатом
                        userService.save(chat,user);
                        userDataCache.setUserBotState(userId,BotState.READY_TO_WORK);
                        log.info("Бот добавлен в канал {} пользователя {}", chat.getTitle(), userId);
                        return SendMessage.builder().chatId(userId).text("Вы присоединили меня к каналу "+chat.getTitle()).build();
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
                                return SendMessage.builder().chatId(pair.getKey()).text("Я больше не админ канала "+chat.getTitle()).build();
                            }
                            if(update.getMyChatMember().getNewChatMember().getStatus().equalsIgnoreCase("kicked")){
                                userService.removeChat(chat.getId().toString());
                                return SendMessage.builder().chatId(pair.getKey()).text(
                                        "Я кикнут с канала "+chat.getTitle()+"чтобы продолжить работу удалите меня из администраторов, и присоедините снова").build();
                            }
                        }
                    }
                    return null ;
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
