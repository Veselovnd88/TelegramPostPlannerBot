package ru.veselov.plannerBot.bots;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.bots.TelegramWebhookBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.controller.UpdateController;
import ru.veselov.plannerBot.utils.BotProperties;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Getter
@Setter
public class MyPreciousBot extends TelegramWebhookBot {

    private final UpdateController updateController;
    private final TelegramBotsApi telegramBotsApi;
    private  final BotProperties botProperties;
    private Long myId;
    @Autowired
    public MyPreciousBot(@Lazy UpdateController updateController, TelegramBotsApi telegramBotsApi, SetWebhook setWebhook, BotProperties botProperties) {
        this.updateController = updateController;
        this.telegramBotsApi = telegramBotsApi;
        this.botProperties = botProperties;
        /*Создание панели меню (слева от строки ввода) бота*/
        BotCommand startCommand = new BotCommand("/start","Приветствие, начало работы с ботом");
        BotCommand createPostCommand = new BotCommand("/create", "Создать новый пост");
        BotCommand seeAllPostsCommand = new BotCommand("/view", "Просмотр постов");
        BotCommand resetCommand = new BotCommand("/reset", "Сбросить");
        BotCommand helpCommand = new BotCommand("/help","Информация о боте");
        List<BotCommand> commandList = new ArrayList<>(List.of(startCommand, createPostCommand, seeAllPostsCommand, resetCommand, helpCommand));
        //Установка меню только для админа
        BotCommandScopeChat botCommandScopeChat = new BotCommandScopeChat();
        botCommandScopeChat.setChatId(Long.valueOf(botProperties.getAdminId()));
        List<BotCommand> commandsAdmin = new ArrayList<>(commandList);
        BotCommand promoteCommand = new BotCommand("/promote","Изменить статус пользователя");
        commandsAdmin.add(promoteCommand);

        try {
            telegramBotsApi.registerBot(this,setWebhook);
            this.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
            this.execute(new SetMyCommands(commandsAdmin,botCommandScopeChat,null));
            log.info("Меню установлено");
            myId=this.getMe().getId();
            log.info("Id бота{}", myId);
        } catch (TelegramApiException e) {
            log.error("Произошла ошибка при запуске бота: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getBotUsername() {
        return botProperties.getName();
    }

    @Override
    public String getBotToken() {
        return botProperties.getToken();
    }



    ////////////////////////
    public synchronized void sendMessageBot(BotApiMethod<?> sendMessage){
            try{
                if(sendMessage instanceof SendMessage){
                    execute(sendMessage);}
                if(sendMessage instanceof AnswerCallbackQuery){
                    execute(sendMessage);
                }
                if(sendMessage instanceof EditMessageText){
                    execute(sendMessage);
                }
                if(sendMessage instanceof EditMessageReplyMarkup){
                    execute(sendMessage);
                }
            }catch (TelegramApiException e){
                log.error(e.getMessage());
            }
        }

    @Override
    public BotApiMethod<?> onWebhookUpdateReceived(Update update) {
        if(update!=null){
            return updateController.processUpdate(update);
        }
        else return null;
    }

    @Override
    public String getBotPath() {
        return botProperties.getWebHookPath();
    }
}
