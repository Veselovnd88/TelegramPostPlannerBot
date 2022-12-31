package ru.veselov.plannerBot.service.postsender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.service.PostService;
import ru.veselov.plannerBot.utils.MessageUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PostSender {
    private final MyPreciousBot bot;

    private final Map<Integer, Timer> timers=new HashMap<>();
    @Autowired
    public PostSender(MyPreciousBot bot) {
        this.bot = bot;
    }


    /*TODO предусмотреть помещение постов в очередь если телеграм отдал ошибку 429 (много постов в 1 секунду)*/
    public void send(Post post) {
        Map<String, SendMediaGroup> groupsCache = new HashMap<>();
        for(Chat chat: post.getChats()) {
            String error = MessageUtils.ERROR_MESSAGE;
            SendMessage errorMessage = new SendMessage(chat.getId().toString(), error);
            log.info("Отправляю пост {} в {} в канал {}",post.getPostId(), post.getDate().toString(),
                    chat.getTitle());
            for(var message: post.getMessages()){
                if(message.hasText()){
                    bot.sendMessageBot(SendMessage.builder()
                                    .chatId(chat.getId())
                            .text(message.getText()).entities(message.getEntities()).build());
                }


                if (message.hasPhoto()){
                    if(message.getMediaGroupId()==null){
                        SendPhoto sendPhoto = new SendPhoto();
                        sendPhoto.setChatId(chat.getId());
                        sendPhoto.setCaption(message.getCaption());
                        sendPhoto.setCaptionEntities(message.getCaptionEntities());
                        sendPhoto.setPhoto(new InputFile(message.getPhoto().get(0).getFileId()));
                        try {
                            bot.execute(sendPhoto);
                        } catch (TelegramApiException e) {
                            log.error("Ошибка при отправке фото");
                        }
                    }
                    else{
                        String mediaGroupId = message.getMediaGroupId();
                        InputMediaPhoto inputMediaPhoto = new InputMediaPhoto(message.getPhoto().get(0).getFileId());
                        inputMediaPhoto.setCaption(message.getCaption());
                        inputMediaPhoto.setCaptionEntities(message.getCaptionEntities());
                        if(groupsCache.containsKey(message.getMediaGroupId())){
                            groupsCache.get(mediaGroupId).getMedias().add(inputMediaPhoto);
                        }
                        else{
                            SendMediaGroup sendMediaGroup = new SendMediaGroup();
                            sendMediaGroup.setChatId(chat.getId());
                            List<InputMedia> list=new LinkedList<>();
                            list.add(inputMediaPhoto);
                            sendMediaGroup.setMedias(list);
                            groupsCache.put(mediaGroupId, sendMediaGroup);
                        }
                        //Проверяем, что все посты из этой группы выбраны, и если да - отправляем группу
                        int groupSize = post.getMessages().stream().filter(x->x.getMediaGroupId()!=null)
                                .map(x -> x.getMediaGroupId()
                                .equals(message.getMediaGroupId())).toList().size();
                        if(groupSize==groupsCache.get(mediaGroupId).getMedias().size()){
                            try {
                                bot.execute(groupsCache.get(mediaGroupId));
                            } catch (TelegramApiException e) {
                                log.error("Ошибка при отправке"); //FIXME exception to mehtod signature
                            }
                        }
                    }
                }
            }
            for (var audio: post.getAudios()){
                SendAudio sendAudio = new SendAudio();
                sendAudio.setChatId(chat.getId());
                if(post.getCaption().containsKey(audio.getFileId())){
                    sendAudio.setCaption(post.getCaption().get(audio.getFileId()));
                }
                sendAudio.setAudio(new InputFile(audio.getFileId()));
                try {
                    bot.execute(sendAudio);
                } catch (TelegramApiException e){
                    log.error("Ошибка при отправке аудиотрека");
                    try {
                        bot.execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        log.info("Ошибка при отправке сообщения об ошибке");
                    }
                }
            }
            for (var doc: post.getDocs()){
                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(chat.getId());
                if(post.getCaption().containsKey(doc.getFileId())){
                    sendDocument.setCaption(post.getCaption().get(doc.getFileId()));
                }
                sendDocument.setDocument(new InputFile(doc.getFileId()));
                try {
                    bot.execute(sendDocument);
                } catch (TelegramApiException e){
                    log.error("Ошибка при отправке документа");
                    try {
                        bot.execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        log.info("Ошибка при отправке сообщения об ошибке");
                    }
                }
            }

            for (var poll : post.getPolls()){
                SendPoll sendPoll = new SendPoll();
                sendPoll.setChatId(chat.getId());
                sendPoll.setQuestion(poll.getQuestion());
                sendPoll.setAllowMultipleAnswers(poll.getAllowMultipleAnswers());
                sendPoll.setType(poll.getType());
                if(poll.getType().equalsIgnoreCase("quiz")){
                    sendPoll.setExplanation(poll.getExplanation());
                    sendPoll.setExplanationEntities(poll.getExplanationEntities());
                }
                sendPoll.setIsAnonymous(poll.getIsAnonymous());
                sendPoll.setOptions(poll.getOptions().stream().map(PollOption::getText).collect(Collectors.toList()));
                sendPoll.setCorrectOptionId(poll.getCorrectOptionId());
                try {
                    bot.execute(sendPoll);
                } catch (TelegramApiException e){
                    e.printStackTrace();
                    log.error("Ошибка при отправке опроса");
                    try {
                        bot.execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        log.info("Ошибка при отправке сообщения об ошибке");
                    }
                }

            }
            for(var video :post.getVideos()){
                SendVideo sendVideo = new SendVideo();
                sendVideo.setChatId(chat.getId());
                if(post.getCaption().containsKey(video.getFileId())){
                    sendVideo.setCaption(post.getCaption().get(video.getFileId()));
                }
                sendVideo.setVideo(new InputFile(video.getFileId()));
                try {
                    bot.execute(sendVideo);
                } catch (TelegramApiException e){
                    log.error("Ошибка при отправке видео");
                    try {
                        bot.execute(errorMessage);
                    } catch (TelegramApiException ex) {
                        log.info("Ошибка при отправке сообщения об ошибке");
                    }
                }
            }

        }

    }

    /*Создается объект таймера и помещается в кеш, на тот случай, если пользоватеть отправил пост в канал
    * нажатием кнопки "Отправить сейчас", чтобы не было повторного вызова таймера*/
    public void createTimer(Post post, PostService postService){
        PostSenderTask postSenderTask = new PostSenderTask(bot, post, postService, this);
        if(timers.containsKey(post.getPostId())){
            Timer savedTimer = timers.get(post.getPostId());
            log.info("Таймер поста {} отменен", post.getPostId());
            savedTimer.purge();
            savedTimer.cancel();
        }
        Timer timer = new Timer();
        timer.schedule(postSenderTask, post.getDate());
        log.info("Пост № {} запланирован к отправке на {}", post.getPostId(), post.getDate());
        timers.put(post.getPostId(),timer);
    }

    public void removeTimer(Integer postId){
        timers.remove(postId);
    }




}
