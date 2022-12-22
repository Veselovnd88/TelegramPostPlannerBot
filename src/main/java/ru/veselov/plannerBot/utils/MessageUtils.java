package ru.veselov.plannerBot.utils;

import com.vdurmont.emoji.EmojiParser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageUtils {
    public static final String DELETED = EmojiParser.parseToUnicode(":fire:Пост удален:fire:");
    public static final String SHOW = EmojiParser.parseToUnicode(":mag:Предпросмотр поста:mag_right:");
    public static final String POST_ID_ERROR = EmojiParser.parseToUnicode(":disappointed:Поста с таким ID не существует," +
            "повторите ввод, или выберите другую команду");
    public static final String POST_SENT = EmojiParser.parseToUnicode("Пост отправлен:grinning:");
    public static String HELP_MESSAGE = EmojiParser.parseToUnicode("""
            :raised_hand:Я бот для планирования постов и публикации в канал\s
            :arrow_backward:Для начала работы и первых инструкций нажмите /start в разделе меню слева\s
            :new:Для создания нового поста нажмите /create и далее следуйте инструкции
            :mag_right:Для просмотра всех постов нажмите /view\s
            :star:Для сброса всех введенных данных нажмите /reset\s
            :gear:Для получения справки нажмите /help\s
            отзывы и предложения: veselovnd@gmail.com""");

    public static String AWAIT_CONTENT_MESSAGE=EmojiParser.parseToUnicode(":grinning:Готов к приему контента(текст, картинки, аудио, видео, файл, опрос)");
    public static String START_MESSAGE =
            EmojiParser.parseToUnicode(":raised_hand:Привет я твой личный Бот:smiley: по планированию постов," +
            " для того чтобы я смог отправлять посты, пожалуйста добавь меня в канал, как администратора");
    public static String START_MESSAGE_USER_ALREADY_USE_BOT = EmojiParser.parseToUnicode(":raised_hand:Привет я твой Бот по планированию постов," +
            " сейчас я администрирую каналы: \n");
    public static String DONT_AWAIT_CONTENT=EmojiParser.parseToUnicode(":neutral_face:Бот занят другими делами");
    public static String BOT_WAS_NOT_ADDED_TO_CHANEL=EmojiParser.parseToUnicode(":disappointed:Бот не присоединен к каналам, нажмите /start для проверки");

    public static String SEE_POSTS = EmojiParser.parseToUnicode(":mag_right:Просмотр постов");
    public static String RESET_POSTS = EmojiParser.parseToUnicode(":exclamation:Создание поста сброшено" +
            ":exclamation:");

    public static String INLINE_BUTTON_WITH_UNKNOWN_DATA=EmojiParser.parseToUnicode(":neutral_face:Нажата кнопка с неизвестным мне названием");

    public static String AWAITING_DATE = EmojiParser.parseToUnicode(":date:Добавляем данные в пост, выберите дату с помощью кнопок и кликните на клавишу Даты/времени" +
            " или введите дату в формате dd.MM.yyyy HH mm");
    public static List<String> AVAILABLE_DATA_FORMATS = List.of("dd.MM.yyyy HH mm");
    public static String POST_SAVED = EmojiParser.parseToUnicode(":white_check_mark:Ваш пост сохранен");

    public static String UNKNOWN_COMMAND=EmojiParser.parseToUnicode(":exclamation:Неизвестная команда");
    public static String ERROR_MESSAGE = EmojiParser.parseToUnicode(":disappointed:Произошла ошибка при отправке");
    public static String POST_LIMIT = EmojiParser.parseToUnicode(":disappointed:Превышен лимит постов"+
            "\n для расширения свяжитесь с администратором, лимит постов=");
    public static String NO_PLANNED_POSTS=EmojiParser.parseToUnicode("Запланированных постов нет");
    public static String shortenString(String title){
        if(title.length()>15){
            return title.substring(0, 7) +"..."+ title.substring(title.length()-7);
        }
        else return title;
    }


}
