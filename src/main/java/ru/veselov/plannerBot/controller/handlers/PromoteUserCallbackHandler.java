package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.service.UserService;

import java.util.Optional;

@Component
@Slf4j
public class PromoteUserCallbackHandler implements UpdateHandler{

    private final UserService userService;
    private final DataCache userDataCache;
    @Autowired
    public PromoteUserCallbackHandler(UserService userService, DataCache userDataCache) {
        this.userService = userService;
        this.userDataCache = userDataCache;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        String data = update.getCallbackQuery().getData();
        //user
        switch (data){
            case "standard":
                System.out.println("Стандарт");
                break;
            case "premium":
                System.out.println("Премиум");
                break;
            case "unlimited":
                System.out.println("Безлимит");
                break;
        }
        return null;
    }
}
