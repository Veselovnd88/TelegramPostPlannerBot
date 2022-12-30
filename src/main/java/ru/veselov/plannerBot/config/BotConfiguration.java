package ru.veselov.plannerBot.config;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.veselov.plannerBot.model.content.MessageDBEntity;
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


    @Bean
    ModelMapper modelMapper(){
        ModelMapper modelMapper=new ModelMapper();
/*        modelMapper.getConfiguration().setPropertyCondition(Conditions.isNotNull());
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        PropertyMap<Message, MessageDBEntity> skipFields= new PropertyMap<Message, MessageDBEntity>() {
            @Override
            protected void configure() {
                skip(source.getFrom());
                skip(source.getMessageId());
            }
        };
        modelMapper.addMappings(skipFields);*/
        return new ModelMapper();
    }
}
