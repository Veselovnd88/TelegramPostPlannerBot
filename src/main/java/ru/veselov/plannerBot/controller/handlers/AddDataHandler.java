package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class AddDataHandler implements UpdateHandler {
    private final DataCache userDataCache;
    @Autowired
    public AddDataHandler(DataCache userDataCache) {
        this.userDataCache = userDataCache;

    }

    @Override
    public SendMessage processUpdate(Update update) {
        Long userId = update.getMessage().getFrom().getId();
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");

            try {
                Date date = sdf.parse(update.getMessage().getText());
                userDataCache.getPostCreator(userId).getPost().setDate(date);
                log.info("Установлена дата поста {} для пользователя {}", date.toString(), userId);
                return savePostAfterInputDate(update.getMessage().getChatId().toString());

            } catch (ParseException e) {
                return new SendMessage(update.getMessage().getChatId().toString(), MessageUtils.AWAITING_DATE);
            }


    }

    public SendMessage savePostAfterInputDate(String chatId){
        SendMessage saveQuestion = new SendMessage();
        saveQuestion.setChatId(chatId);
        saveQuestion.setText("Готов сохранить пост");
        saveQuestion.enableMarkdown(true);

        InlineKeyboardButton pictureYes = new InlineKeyboardButton();
        pictureYes.setText("Сохранить");
        pictureYes.setCallbackData("saveYes");
        List<InlineKeyboardButton> row1 = new ArrayList<>(List.of(pictureYes));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowList);
        saveQuestion.setReplyMarkup(inlineKeyboardMarkup);
        return saveQuestion;
    }
}
