package ru.veselov.plannerBot.service.postsender;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.media.*;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.service.PostService;

import java.util.*;
import java.util.concurrent.TimeUnit;
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

    public void send(Post post) throws TelegramApiException {
        Map<String, SendMediaGroup> groupsCache = new HashMap<>();
        for(Chat chat: post.getChats()) {
            log.info("Отправляю пост {} в {} в канал {}",post.getPostId(), post.getDate().toString(),
                    chat.getTitle());
            for(var message: post.getMessages()){
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
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
                        bot.execute(sendPhoto);
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
                            SendMediaGroup sendMediaGroup = createSendMediaGroup(chat, inputMediaPhoto);
                            groupsCache.put(mediaGroupId, sendMediaGroup);
                        }
                        //Проверяем, что все посты из этой группы выбраны, и если да - отправляем группу
                        if(checkIfMediaGroupReadyToSend(post,message,groupsCache.get(mediaGroupId))){
                            bot.execute(groupsCache.get(mediaGroupId));
                        }
                    }
                }
                if (message.hasDocument()){
                    if(message.getMediaGroupId()==null){
                        SendDocument sendDocument = new SendDocument();
                        sendDocument.setChatId(chat.getId());
                        sendDocument.setCaption(message.getCaption());
                        sendDocument.setCaptionEntities(message.getCaptionEntities());
                        sendDocument.setDocument(new InputFile(message.getDocument().getFileId()));
                        bot.execute(sendDocument);

                    }
                    else{
                        String mediaGroupId = message.getMediaGroupId();
                        InputMediaDocument inputMediaDocument = new InputMediaDocument(message.getDocument().getFileId());
                        inputMediaDocument.setCaption(message.getCaption());
                        inputMediaDocument.setCaptionEntities(message.getCaptionEntities());
                        if(groupsCache.containsKey(message.getMediaGroupId())){
                            groupsCache.get(mediaGroupId).getMedias().add(inputMediaDocument);
                        }
                        else{
                            SendMediaGroup sendMediaGroup = createSendMediaGroup(chat, inputMediaDocument);
                            groupsCache.put(mediaGroupId, sendMediaGroup);
                        }
                        if(checkIfMediaGroupReadyToSend(post,message,groupsCache.get(mediaGroupId))){
                                bot.execute(groupsCache.get(mediaGroupId));
                        }
                    }
                }
                if (message.hasAudio()){
                    if(message.getMediaGroupId()==null){
                        SendAudio sendAudio = new SendAudio();
                        sendAudio.setChatId(chat.getId());
                        sendAudio.setCaption(message.getCaption());
                        sendAudio.setCaptionEntities(message.getCaptionEntities());
                        sendAudio.setAudio(new InputFile(message.getAudio().getFileId()));
                        bot.execute(sendAudio);
                    }
                    else{
                        String mediaGroupId = message.getMediaGroupId();
                        InputMediaAudio inputMediaAudio = new InputMediaAudio(message.getAudio().getFileId());
                        inputMediaAudio.setCaption(message.getCaption());
                        inputMediaAudio.setCaptionEntities(message.getCaptionEntities());
                        if(groupsCache.containsKey(message.getMediaGroupId())){
                            groupsCache.get(mediaGroupId).getMedias().add(inputMediaAudio);
                        }
                        else{
                            SendMediaGroup sendMediaGroup = createSendMediaGroup(chat, inputMediaAudio);
                            groupsCache.put(mediaGroupId, sendMediaGroup);
                        }
                        if(checkIfMediaGroupReadyToSend(post,message,groupsCache.get(mediaGroupId))){
                            bot.execute(groupsCache.get(mediaGroupId));
                        }
                    }
                }
                if (message.hasVideo()){
                    if(message.getMediaGroupId()==null){
                        SendVideo sendVideo = new SendVideo();
                        sendVideo.setChatId(chat.getId());
                        sendVideo.setCaption(message.getCaption());
                        sendVideo.setCaptionEntities(message.getCaptionEntities());
                        sendVideo.setVideo(new InputFile(message.getVideo().getFileId()));
                        bot.execute(sendVideo);
                    }
                    else{
                        String mediaGroupId = message.getMediaGroupId();
                        InputMediaVideo inputMediaVideo= new InputMediaVideo(message.getVideo().getFileId());
                        inputMediaVideo.setCaption(message.getCaption());
                        inputMediaVideo.setCaptionEntities(message.getCaptionEntities());
                        if(groupsCache.containsKey(message.getMediaGroupId())){
                            groupsCache.get(mediaGroupId).getMedias().add(inputMediaVideo);
                        }
                        else{
                            SendMediaGroup sendMediaGroup = createSendMediaGroup(chat, inputMediaVideo);
                            groupsCache.put(mediaGroupId, sendMediaGroup);
                        }
                        if(checkIfMediaGroupReadyToSend(post,message,groupsCache.get(mediaGroupId))){
                            bot.execute(groupsCache.get(mediaGroupId));
                        }
                    }
                }
                if (message.hasAnimation()){
                    if(message.getMediaGroupId()==null){
                        SendAnimation sendAnimation = new SendAnimation();
                        sendAnimation.setChatId(chat.getId());
                        sendAnimation.setCaption(message.getCaption());
                        sendAnimation.setCaptionEntities(message.getCaptionEntities());
                        sendAnimation.setAnimation(new InputFile(message.getAnimation().getFileId()));
                        bot.execute(sendAnimation);
                    }
                    else{
                        String mediaGroupId = message.getMediaGroupId();
                        InputMediaAnimation inputMediaAnimation= new InputMediaAnimation(message.getAnimation().getFileId());
                        inputMediaAnimation.setCaption(message.getCaption());
                        inputMediaAnimation.setCaptionEntities(message.getCaptionEntities());
                        if(groupsCache.containsKey(message.getMediaGroupId())){
                            groupsCache.get(mediaGroupId).getMedias().add(inputMediaAnimation);
                        }
                        else{
                            SendMediaGroup sendMediaGroup = createSendMediaGroup(chat, inputMediaAnimation);
                            groupsCache.put(mediaGroupId, sendMediaGroup);
                        }
                        if(checkIfMediaGroupReadyToSend(post,message,groupsCache.get(mediaGroupId))){
                            bot.execute(groupsCache.get(mediaGroupId));
                        }
                    }
                }
                if (message.hasPoll()){
                    Poll poll = message.getPoll();
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
                    sendPoll.setIsClosed(poll.getIsClosed());
                    sendPoll.setCloseDate(poll.getCloseDate());
                    sendPoll.setOpenPeriod(poll.getOpenPeriod());
                    bot.execute(sendPoll);
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
    /*Функция проверяет, что все сообщения с одной медиагруппой собраны в объект SendMediaGroup
    * и что пора ее отправлять*/
    private boolean checkIfMediaGroupReadyToSend(Post post, Message message, SendMediaGroup sendMediaGroup){
        int groupSize = post.getMessages().stream().filter(x->x.getMediaGroupId()!=null)
                .map(x -> x.getMediaGroupId()
                        .equals(message.getMediaGroupId())).toList().size();
        return (groupSize==sendMediaGroup.getMedias().size());
    }
    private SendMediaGroup createSendMediaGroup(Chat chat,InputMedia inputMedia){
        SendMediaGroup sendMediaGroup = new SendMediaGroup();
        sendMediaGroup.setChatId(chat.getId());
        List<InputMedia> list=new LinkedList<>();
        list.add(inputMedia);
        sendMediaGroup.setMedias(list);
        return sendMediaGroup;
    }



}
