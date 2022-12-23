package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.model.UserStatus;
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
    /*Изменение статуса пользователя*/
    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        String data = update.getCallbackQuery().getData();
        Optional<UserEntity> userById = userService.findUserById(userDataCache.getPromoteUser());
        if(userById.isPresent()){
            UserEntity userEntity = userById.get();
            switch (data){
                case "standard":
                    userEntity.setStatus(UserStatus.STANDARD);
                    break;
                case "premium":
                    userEntity.setStatus(UserStatus.PREMIUM);
                    break;
                case "unlimited":
                    userEntity.setStatus(UserStatus.UNLIMITED);
                    break;
            }
            userService.saveUser(userEntity);
            log.info("Изменен статус пользователя {}", userEntity.getUserId());
            userDataCache.setUserBotState(update.getCallbackQuery().getFrom().getId(), BotState.READY_TO_WORK);
            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                    .text("Статус успешно обновлен на "+ userEntity.getStatus()).build();
        }
        userDataCache.setUserBotState(update.getCallbackQuery().getFrom().getId(), BotState.READY_TO_WORK);
        return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                .text("Такого пользователя нет в базе").build();
    }
}
