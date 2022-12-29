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
import ru.veselov.plannerBot.bots.BotState;
import ru.veselov.plannerBot.controller.UpdateHandler;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static ru.veselov.plannerBot.utils.MessageUtils.AWAITING_DATE;

@Component
@Slf4j
public class ChooseDateHandler implements UpdateHandler {

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
                //проверка ручного ввода
                if(update.hasMessage()&&update.getMessage().hasText()){
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");
                        Date date = sdf.parse(update.getMessage().getText());
                        return savePostAfterInputDate(update.getMessage().getChatId().toString(), date, userId);

                    } catch (ParseException e) {
                        return new SendMessage(update.getMessage().getChatId().toString(), MessageUtils.AWAITING_DATE);
                        }
                }
                //коллбэки
                String rawData = update.getCallbackQuery().getData();
                String data = rawData.split(":")[0];
                Calendar refCalendar = userDataCache.getSavedDate().get(userId);
                switch (data){
                    //Нажатие клавиши с отображением времени/даты - прикрепляет дату к посту
                    case "chosenDay":
                    case "chosenTime":
                        return savePostAfterInputDate(update.getCallbackQuery().getFrom().getId().toString(),
                                userDataCache.getSavedDate().get(userId).getTime(), userId);
                    //клавиши управления датой и временем
                    case "dayLeft":
                        refCalendar.add(Calendar.DAY_OF_MONTH,-1);
                        if(refCalendar.getTime().before(userDataCache.getStartedDate().get(userId))){
                            refCalendar.add(Calendar.DAY_OF_MONTH,1);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать вчерашний день")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "dayLeft7":
                        refCalendar.add(Calendar.DAY_OF_MONTH,-7);
                        if(refCalendar.getTime().before(new Date())){
                            refCalendar.add(Calendar.DAY_OF_MONTH,7);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать день в прошлом")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "dayRight":
                        refCalendar.add(Calendar.DAY_OF_MONTH,+1);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "dayRight7":

                        refCalendar.add(Calendar.DAY_OF_MONTH,+7);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "hourLeft":
                        refCalendar.add(Calendar.HOUR_OF_DAY,-1);
                        if(refCalendar.getTime().before(userDataCache.getStartedDate().get(userId))){
                            refCalendar.add(Calendar.HOUR_OF_DAY,1);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать время раньше текущего")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "hourRight":
                        refCalendar.add(Calendar.HOUR_OF_DAY,+1);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "minutesLeft":
                        refCalendar.add(Calendar.MINUTE,-1);
                        if(refCalendar.getTime().before(new Date())){
                            refCalendar.add(Calendar.MINUTE,1);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать время раньше текущего")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "minutesLeft10":
                        refCalendar.add(Calendar.MINUTE,-10);
                        if(refCalendar.getTime().before(new Date())){
                            refCalendar.add(Calendar.MINUTE,10);
                            return AnswerCallbackQuery.builder().callbackQueryId(update.getCallbackQuery().getId())
                                    .text("Вы не можете выбрать время раньше текущего")
                                    .showAlert(true).build();
                        }
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "minutesRight":
                        refCalendar.add(Calendar.MINUTE,+1);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);

                    case "minutesRight10":
                        refCalendar.add(Calendar.MINUTE,+10);
                        userDataCache.getSavedDate().put(userId,refCalendar);
                        return  editMessage(update,refCalendar);
                }
        }
        return null;
    }

    ////////Клавиатуры//////////
    private InlineKeyboardMarkup setKeyBoardChoseDate(String showDate, String showTime){
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        //Даты
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
        List<InlineKeyboardButton> rowDay = new ArrayList<>(List.of(today));
        List<InlineKeyboardButton> arrows = new ArrayList<>(List.of(dayLeftArrow7,dayLeftArrow, dayRightArrow,dayRightArrow7));
        rows.add(rowDay);
        rows.add(arrows);
        //Часы
        var time = new InlineKeyboardButton();
        time.setText(showTime);
        time.setCallbackData("chosenTime:"+time.getText());
        var timeLeftArrow = new InlineKeyboardButton();
        timeLeftArrow.setText("<Ч");
        timeLeftArrow.setCallbackData("hourLeft");
        var timeRightArrow = new InlineKeyboardButton();
        timeRightArrow.setText("Ч>");
        timeRightArrow.setCallbackData("hourRight");
        List<InlineKeyboardButton> rowTime = new ArrayList<>(List.of(timeLeftArrow,time,timeRightArrow));
        //Минуты
        var minutesLeftArrow = new InlineKeyboardButton();
        minutesLeftArrow.setText("<M");
        minutesLeftArrow.setCallbackData("minutesLeft");
        var minutesLeftArrow10 = new InlineKeyboardButton();
        minutesLeftArrow10.setText("<<M");
        minutesLeftArrow10.setCallbackData("minutesLeft10");
        var minutesRightArrow = new InlineKeyboardButton();
        minutesRightArrow.setText("M>");
        minutesRightArrow.setCallbackData("minutesRight");
        var minutesRightArrow10 = new InlineKeyboardButton();
        minutesRightArrow10.setText("M>>");
        minutesRightArrow10.setCallbackData("minutesRight10");
        List<InlineKeyboardButton> rowArrowsMinutes = new ArrayList<>(List.of(minutesLeftArrow10,minutesLeftArrow,minutesRightArrow,minutesRightArrow10));
        rows.add(rowTime);
        rows.add(rowArrowsMinutes);
        ////////
        inlineKeyboardMarkup.setKeyboard(rows);
        return inlineKeyboardMarkup;
    }

    private SendMessage savePostAfterInputDate(String chatId, Date date, Long userId){
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH mm");
        userDataCache.getPostCreator(userId).getPost().setDate(date);
        log.info("Установлена дата поста {} для пользователя {}", date.toString(), userId);
        userDataCache.setUserBotState(userId, BotState.READY_TO_SAVE);
        SendMessage saveQuestion = new SendMessage();
        saveQuestion.setChatId(chatId);
        saveQuestion.setText("Пост будет отправлен: "+ sdf.format(date)+" сохранить?");
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

    //Вспомогательные методы
    private String getDisplayDate(Calendar calendar){
        return calendar.get(Calendar.DAY_OF_MONTH) +"\n"+ calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, locale);
    }
    private String getDisplayTime(Calendar calendar){
        return String.format("%02d:%02d",calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE));
    }

    private EditMessageReplyMarkup editMessage(Update update, Calendar refCalendar){
        return  EditMessageReplyMarkup.builder()
                .chatId(update.getCallbackQuery().getMessage().getChatId().toString())
                .messageId(update.getCallbackQuery().getMessage().getMessageId())
                .replyMarkup(setKeyBoardChoseDate(getDisplayDate(refCalendar),getDisplayTime(refCalendar)))
                .build();
    }

}
