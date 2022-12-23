package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.veselov.plannerBot.cache.DataCache;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PromoteUserTextHandler  implements  UpdateHandler{

    private final DataCache userDataCache;
    @Autowired
    public PromoteUserTextHandler(DataCache userDataCache) {
        this.userDataCache = userDataCache;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        userDataCache.setPromoteUser(update.getMessage().getForwardFrom());
        return promoteMessage(update.getMessage().getFrom().getId());
    }


    private SendMessage promoteMessage(Long userId){
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton standardButton = new InlineKeyboardButton();
        standardButton.setCallbackData("standard");
        standardButton.setText("Стандарт");
        InlineKeyboardButton premiumButton = new InlineKeyboardButton();
        premiumButton.setCallbackData("premium");
        premiumButton.setText("Премиум");
        InlineKeyboardButton unlimButton = new InlineKeyboardButton();
        unlimButton.setCallbackData("unlimited");
        unlimButton.setText("Безлимитный>");
        row1.add(standardButton);
        row2.add(premiumButton);
        row3.add(unlimButton);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        rowList.add(row2);
        rowList.add(row3);
        inlineKeyboardMarkup.setKeyboard(rowList);
        return SendMessage.builder().chatId(userId).replyMarkup(inlineKeyboardMarkup)
                .text("Выберите статус").build();

    }
}
