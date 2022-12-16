package ru.veselov.plannerBot.controller.handlers;

import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface UpdateHandler {

    BotApiMethod<?> processUpdate(Update update);


}
