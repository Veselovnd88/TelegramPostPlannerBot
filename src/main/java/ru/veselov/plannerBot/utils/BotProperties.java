package ru.veselov.plannerBot.utils;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot")
public class BotProperties {
    private String name;
    private String token;
    private String webHookPath;
    private String adminId;

}
