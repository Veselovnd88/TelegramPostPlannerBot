package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.BotState;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ru.veselov.plannerBot.utils.MessageUtils.AWAITING_DATE;

@Component
@Slf4j
public class ChooseDateHandler implements UpdateHandler{

    private final Locale locale = new Locale("ru");

    private final DataCache userDataCache;
    @Autowired
    public ChooseDateHandler(DataCache userDataCache) {
        this.userDataCache = userDataCache;
    }


    @Override
    public BotApiMethod<?> processUpdate(Update update) {
        Long userId=null;
        if(update.hasCallbackQuery()){
            userId = update.getCallbackQuery().getFrom().getId();
        }
        else if (update.hasMessage()){
            userId = update.getMessage().getFrom().getId();
        }
        BotState botState = userDataCache.getUsersBotState(userId);
        switch (botState){
            case AWAITING_POST:
                SendMessage awaitingDateMessage =
                        new SendMessage(update.getCallbackQuery().getMessage().getChatId().toString(),
                                AWAITING_DATE);
                userDataCache.setUserBotState(userId, BotState.AWAITING_DATE);
                //получили текущую дату и время, передали в клавиатуру для показа пользователю, от нее будет шагать влево и вправо
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeZone(TimeZone.getTimeZone("Europe/Moscow"));
                String displayName = getDisplayDate(calendar);
                String displayTime = getDisplayTime(calendar);
                //поместили в кеш, для сверки с переходом по стрелочкам
                userDataCache.getSavedDate().put(userId,calendar);
                userDataCache.getStartedDate().put(userId,calendar.getTime());
                awaitingDateMessage.enableMarkdown(true);
                awaitingDateMessage.setReplyMarkup(setKeyBoardChoseDate(displayName, displayTime));
                return awaitingDateMessage;


            case AWAITING_DATE:
                if(update.hasMessage()&&update.getMessage().hasText()){
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");
                try {
                    Date date = sdf.parse(update.getMessage().getText());
                    return savePostAfterInputDate(update.getMessage().getChatId().toString(), date, userId);

                } catch (ParseException e) {
                    return new SendMessage(update.getMessage().getChatId().toString(), MessageUtils.AWAITING_DATE);
                    }
                }

                String rawData = update.getCallbackQuery().getData();
                String data = rawData.split(":")[0];
                Calendar refCalendar = userDataCache.getSavedDate().get(userId);
                switch (data){
                    case "chosenDay":
                        return savePostAfterInputDate(update.getCallbackQuery().getFrom().getId().toString(),
                                userDataCache.getSavedDate().get(userId).getTime(), userId);
                    case "dayLeft":
                        refCalendar.add(Calendar.DAY_OF_MONTH,-1);
                        if(refCalendar.getTime().before(userDataCache.getStartedDate().get(userId))){
                            refCalendar.add(Calendar.DAY_OF_MONTH,1);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать вчерашний день")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  EditMessageReplyMarkup.builder()
                                .chatId(update.getCallbackQuery().getMessage().getChatId().toString())
                                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                                .replyMarkup(setKeyBoardChoseDate(getDisplayDate(refCalendar),getDisplayTime(refCalendar)))
                                .build();
                    case "dayLeft7":
                        refCalendar.add(Calendar.DAY_OF_MONTH,-7);
                        if(refCalendar.getTime().before(new Date())){
                            refCalendar.add(Calendar.DAY_OF_MONTH,7);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать день в прошлом")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  EditMessageReplyMarkup.builder()
                                .chatId(update.getCallbackQuery().getMessage().getChatId().toString())
                                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                                .replyMarkup(setKeyBoardChoseDate(getDisplayDate(refCalendar),getDisplayTime(refCalendar)))
                                .build();
                    case "dayRight":
                        refCalendar.add(Calendar.DAY_OF_MONTH,+1);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  EditMessageReplyMarkup.builder()
                                .chatId(update.getCallbackQuery().getMessage().getChatId().toString())
                                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                                .replyMarkup(setKeyBoardChoseDate(getDisplayDate(refCalendar),getDisplayTime(refCalendar)))
                                .build();
                    case "dayRight7":
                        refCalendar.add(Calendar.DAY_OF_MONTH,+7);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  EditMessageReplyMarkup.builder()
                                .chatId(update.getCallbackQuery().getMessage().getChatId().toString())
                                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                                .replyMarkup(setKeyBoardChoseDate(getDisplayDate(refCalendar),getDisplayTime(refCalendar)))
                                .build();




                    case "chosenTime":
                        System.out.println("Время"+data+ rawData.split(":")[1]);
                        return null;
                    case "timeLeft":
                        System.out.println("Время налево");
                        return null;
                    case "timeRight":
                        System.out.println("Время направо");
                        return null;

                }
        }
        return null;
    }




    private InlineKeyboardMarkup setKeyBoardChoseDate(String showDate, String showTime){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        //////Даты
        var  today  = new InlineKeyboardButton();
        today.setText(showDate);
        today.setCallbackData("chosenDay:"+today.getText());
        var dayLeftArrow = new InlineKeyboardButton();
        dayLeftArrow.setText("<");
        dayLeftArrow.setCallbackData("dayLeft");
        var dayLeftArrow7 = new InlineKeyboardButton();
        dayLeftArrow7.setText("<<");
        dayLeftArrow7.setCallbackData("dayLeft7");
        var dayRightArrow = new InlineKeyboardButton();
        dayRightArrow.setText(">");
        dayRightArrow.setCallbackData("dayRight");
        var dayRightArrow7 = new InlineKeyboardButton();
        dayRightArrow7.setText(">>");
        dayRightArrow7.setCallbackData("dayRight7");
        List<InlineKeyboardButton> rowDay = new ArrayList<>(List.of(dayLeftArrow7,dayLeftArrow, today, dayRightArrow,dayRightArrow7));
        rows.add(rowDay);
        ///////////////
        var time = new InlineKeyboardButton();
        time.setText(showTime);
        time.setCallbackData("chosenTime:"+time.getText());
        var timeLeftArrow = new InlineKeyboardButton();
        timeLeftArrow.setText("<<");
        timeLeftArrow.setCallbackData("timeLeft");
        var timeRightArrow = new InlineKeyboardButton();
        timeRightArrow.setText(">>");
        timeRightArrow.setCallbackData("timeRight");
        List<InlineKeyboardButton> rowTime = new ArrayList<>(List.of(timeLeftArrow,time,timeRightArrow));
        rows.add(rowTime);
        ////////
        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }


    private String getDisplayDate(Calendar calendar){
        return calendar.get(Calendar.DAY_OF_MONTH) +" "+ calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale);
    }
    private String getDisplayTime(Calendar calendar){
        return calendar.get(Calendar.HOUR_OF_DAY)+ " "+ calendar.get(Calendar.MINUTE);
    }



    public SendMessage savePostAfterInputDate(String chatId, Date date, Long userId){
        userDataCache.getPostCreator(userId).getPost().setDate(date);
        log.info("Установлена дата поста {} для пользователя {}", date.toString(), userId);
        userDataCache.setUserBotState(userId, BotState.READY_TO_SAVE);
        SendMessage saveQuestion = new SendMessage();
        saveQuestion.setChatId(chatId);
        saveQuestion.setText("Готов сохранить пост");
        saveQuestion.enableMarkdown(true);
        InlineKeyboardButton saveButton = new InlineKeyboardButton();
        saveButton.setText("Сохранить");
        saveButton.setCallbackData("saveYes");
        InlineKeyboardButton inputDate = new InlineKeyboardButton();
        inputDate.setText("Выбрать другое время");
        inputDate.setCallbackData("inputDate");
        List<InlineKeyboardButton> row1 = new ArrayList<>(List.of(saveButton));
        List<InlineKeyboardButton> row2 = new ArrayList<>(List.of(inputDate));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        rowList.add(row2);
        inlineKeyboardMarkup.setKeyboard(rowList);
        saveQuestion.setReplyMarkup(inlineKeyboardMarkup);
        return saveQuestion;
    }
}
