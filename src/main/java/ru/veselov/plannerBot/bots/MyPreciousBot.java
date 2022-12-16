package ru.veselov.plannerBot.bots;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.controller.UpdateController;
import ru.veselov.plannerBot.utils.BotProperties;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Getter
@Setter
public class MyPreciousBot extends TelegramLongPollingBot {

    private final UpdateController updateController;
    private final TelegramBotsApi telegramBotsApi;
    private  final BotProperties botProperties;
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


        try {
            telegramBotsApi.registerBot(this);
            this.execute(new SetMyCommands(commandList, new BotCommandScopeDefault(), null));
            log.info("Меню установлено");
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

    @Override
    public void onUpdateReceived(Update update) {
        updateController.processUpdate(update);
    }


    ////////////////////////
    public synchronized void sendMessageBot(BotApiMethod<?> sendMessage){
            try{
                if(sendMessage instanceof SendMessage){
                    execute(sendMessage);}
                if(sendMessage instanceof AnswerCallbackQuery){
                    execute(sendMessage);
                }
            }catch (TelegramApiException e){
                e.printStackTrace();
            }
        }
}
