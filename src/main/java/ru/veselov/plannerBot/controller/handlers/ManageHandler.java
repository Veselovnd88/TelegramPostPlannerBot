package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ManageHandler implements UpdateHandler{
    private final PostService postService;
    private final DataCache userDataCache;

    public ManageHandler(PostService postService, DataCache userDataCache) {
        this.postService = postService;
        this.userDataCache = userDataCache;
    }

    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        String num = update.getMessage().getText();
        int id;
        try{
            id = Integer.parseInt(num);
            if(postService.existsByPostId(id)){
                userDataCache.addPostForManage(update.getMessage().getFrom().getId(),id);
                return (manageMessage(update.getMessage().getFrom().getId()));
            }
            else {
                return new SendMessage(update.getMessage().getFrom().getId().toString(),
                        MessageUtils.POST_ID_ERROR);
            }
        }
        catch (NumberFormatException e){
            return (new SendMessage(update.getMessage().getFrom().getId().toString(),
                    MessageUtils.POST_ID_ERROR));
        }
    }
    private SendMessage manageMessage(Long userId){
        SendMessage message = new SendMessage();
        message.setChatId(userId);
        message.enableMarkdown(true);
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setCallbackData("delete");
        deleteButton.setText("Удалить пост");
        InlineKeyboardButton seeButton = new InlineKeyboardButton();
        seeButton.setCallbackData("view");
        seeButton.setText("Просмотреть пост");
        row1.add(seeButton);
        row1.add(deleteButton);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        inlineKeyboardMarkup.setKeyboard(rowList);
        message.setReplyMarkup(inlineKeyboardMarkup);
        message.setChatId(userId);
        message.setText("Пост "+userDataCache.getPostForManage(userId));
        return message;
    }
}
