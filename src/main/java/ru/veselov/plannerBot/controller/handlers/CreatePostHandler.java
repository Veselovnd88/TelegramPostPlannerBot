package ru.veselov.plannerBot.controller.handlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.games.Animation;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.veselov.plannerBot.cache.DataCache;
import ru.veselov.plannerBot.controller.UpdateHandler;
import ru.veselov.plannerBot.service.UserService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class CreatePostHandler implements UpdateHandler {
    private final UserService userService;
    private final DataCache userDataCache;

    @Autowired
    public CreatePostHandler(UserService userService, DataCache userDataCache) {
        this.userService = userService;
        this.userDataCache = userDataCache;
    }

    public SendMessage processUpdate(Update update){
        //Проверка на то, что пост не содержит более 15 сообщений
        // (API телеграма не позволяет отправлять в чат более 20 сообщений в минуту
        Long userId=update.getMessage().getFrom().getId();
        if(userDataCache.getPost(userId).getMessages().size()>14){
            SendMessage addContentMessage = askAddContent(update);
            addContentMessage.setText("Превышено максимальное количество сообщений для отправки в канал(15)");
            return addContentMessage;
        }
        //Проверка на длину подписи (caption) бот не может отправлять сообщение длинней 1024 символов
        if(update.getMessage().getCaption()!=null){
            if(update.getMessage().getCaption().length()>1024){
                return SendMessage.builder().chatId(userId)
                        .text(MessageUtils.CAPTION_TOO_LONG).build();
            }
        }
        //Проверка на кастомные эмодзи
        if(update.getMessage().getEntities()!=null){
            List<MessageEntity> entities = update.getMessage().getEntities();
            if(entities.stream().anyMatch(x->x.getType().equals("custom_emoji"))){
                return SendMessage.builder().chatId(userId)
                        .text(MessageUtils.NO_CUSTOM_EMOJI).build();
            }
        }
        //Сохранение текста с параметрами форматирования
        if(update.getMessage().hasText()){
            Message message = new Message();
            String text = update.getMessage().getText();
            message.setText(text);
            message.setEntities(update.getMessage().getEntities());
            userDataCache.getPost(userId).addMessage(message);
            log.info("Сохранен текст с разметкой для пользователя {}",userId);
            }
            //TODO сохранение файлов в форме byte[] в базе данных если решим сохранять файлы
        if(update.getMessage().hasPhoto()){
            Message message = createMessageWithMedia(update);
            //Получаем список из нескольких вариантов картинки разных размеров
            List<PhotoSize> photoSizes = update.getMessage().getPhoto();
            PhotoSize photoSize = photoSizes.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElse(null);
            if(photoSize==null){
                return SendMessage.builder().chatId(userId).text(MessageUtils.CANT_GET_PICTURE).build();
            }
            message.setPhoto(List.of(photoSize));
            log.info("Сохранил картинку в пост для юзера {}", userId);
            userDataCache.getPost(userId).addMessage(message);
        }
        if(update.getMessage().hasAudio()){
            Message message = createMessageWithMedia(update);
            Audio audio = update.getMessage().getAudio();
            message.setAudio(audio);
            log.info("Сохранил аудиотрек в пост для юзера {}",userId);
            userDataCache.getPost(userId).addMessage(message);
        }
        if(update.getMessage().hasDocument()){
            Message message = createMessageWithMedia(update);
            Document document = update.getMessage().getDocument();
            message.setDocument(document);
            log.info("Сохранил документ в пост для юзера {}",userId);
            userDataCache.getPost(userId).addMessage(message);
        }
        if(update.getMessage().hasVideo()){
            Message message = createMessageWithMedia(update);
            Video video = update.getMessage().getVideo();
            message.setVideo(video);
            log.info("Сохранил видео в пост для юзера {}", userId);
            userDataCache.getPost(userId).addMessage(message);
        }
        if(update.getMessage().hasAnimation()){
            Message message = createMessageWithMedia(update);
            Animation animation = update.getMessage().getAnimation();
            message.setAnimation(animation);
            log.info("Сохранил видео в пост для юзера {}", userId);
            userDataCache.getPost(userId).addMessage(message);
        }

        if(update.getMessage().hasPoll()){
            Message message = new Message();
            Poll poll = update.getMessage().getPoll();
            message.setPoll(poll);
            log.info("Сохранил опрос в пост для юзера {}", userId);
            userDataCache.getPost(userId).addMessage(message);
            }
            return askAddContent(update);
     }

    private SendMessage askAddContent(Update update) {
        SendMessage contentQuestion = new SendMessage();
        contentQuestion.setChatId(update.getMessage().getChatId());
        contentQuestion.setText(MessageUtils.AWAIT_CONTENT_MESSAGE);
        contentQuestion.enableMarkdown(true);
        Set<Chat> chats = userService.findAllChatsByUser(update.getMessage().getFrom());
        List<InlineKeyboardButton> row11 = new ArrayList<>();
        for(Chat chat: chats){
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(chat.getTitle());
            button.setCallbackData(MessageUtils.shortenString(chat.getTitle()));
            row11.add(button);
        }
        InlineKeyboardButton pictureYes = new InlineKeyboardButton();

        pictureYes.setText("Опубликовать во все каналы");
        pictureYes.setCallbackData("postAll");

        List<InlineKeyboardButton> row1 = new ArrayList<>(List.of(pictureYes));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(row1);
        rowList.add(row11);
        inlineKeyboardMarkup.setKeyboard(rowList);
        contentQuestion.setReplyMarkup(inlineKeyboardMarkup);
        return contentQuestion;
    }

    private Message createMessageWithMedia(Update update){
        Message message = new Message();
        message.setCaption(update.getMessage().getCaption());
        message.setCaptionEntities(update.getMessage().getCaptionEntities());
        message.setMediaGroupId(update.getMessage().getMediaGroupId());
        return message;
    }

}
