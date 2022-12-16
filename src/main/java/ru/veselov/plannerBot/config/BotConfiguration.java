package ru.veselov.plannerBot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.veselov.plannerBot.utils.BotProperties;

@Slf4j
@Configuration
public class BotConfiguration {
    private final BotProperties botProperties;

    @Autowired
    public BotConfiguration(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi(){
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            log.info("Экземпляр Api создан");
            return telegramBotsApi;
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    @Bean
    SetWebhook setWebhookInstance(){
        return SetWebhook.builder().url(botProperties.getWebHookPath()).build();
    }
}
