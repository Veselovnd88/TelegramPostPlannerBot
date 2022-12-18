package ru.veselov.plannerBot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;
import org.telegram.telegrambots.meta.api.objects.polls.PollOption;
import ru.veselov.plannerBot.bots.MyPreciousBot;
import ru.veselov.plannerBot.model.Post;
import ru.veselov.plannerBot.model.PostEntity;
import ru.veselov.plannerBot.model.PostState;
import ru.veselov.plannerBot.model.UserEntity;
import ru.veselov.plannerBot.model.content.*;
import ru.veselov.plannerBot.repository.ChatRepository;
import ru.veselov.plannerBot.repository.PostRepository;
import ru.veselov.plannerBot.repository.UserRepository;
import ru.veselov.plannerBot.service.postsender.PostSender;
import ru.veselov.plannerBot.service.postsender.PostSenderTask;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
public class PostService {
    private final MyPreciousBot bot;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final UserService userService;
    private final PostSender postSender;

    @Autowired
    public PostService(MyPreciousBot bot, PostRepository postRepository, UserRepository userRepository, ChatRepository chatRepository, UserService userService, PostSender postSender) {
        this.bot = bot;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.userService = userService;
        this.postSender = postSender;
    }
    /*Планировщик постов, сюда попадает ДТО в случаях:
    * 1) Сразу после создания - со статусом CREATE, проверяется время публикации внутри метода
    * 2) Из планировщика Спринг - при выборке из БД с условием SAVED и не позднее +1 день после старта*/
    @Transactional
    public void planPost(Post postDto){
        Calendar cl=Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        PostState postState = postDto.getPostState();
        User user = postDto.getUser();
        cl.add(Calendar.DATE,1);
        //Для связки юзера и поста - получаем из бд
        Optional<UserEntity> userOptional = userRepository.findByUserId(user.getId().toString());
        //Если юзер существует, то сразу проверяем дату поста
        if(userOptional.isPresent()){
            PostEntity postEntity;
            if(postDto.getDate().before(cl.getTime())){
                if(postState==PostState.CREATED){
                    postEntity=convertToEntity(postDto);
                    postEntity.setPostState(PostState.PLANNED);
                }
                else if(postState==PostState.SAVED){
                    Optional<PostEntity> optionalPostEntity = postRepository.findById(postDto.getPostId());
                    postEntity = optionalPostEntity.orElseGet(() -> convertToEntity(postDto));
                    postEntity.setPostState(PostState.PLANNED);
                }
                else if(postState==PostState.PLANNED){
                    Optional<PostEntity> optionalPostEntity =postRepository.findById(postDto.getPostId());
                    postEntity=optionalPostEntity.orElseGet(()->convertToEntity(postDto));
                }
                else{
                    postEntity=convertToEntity(postDto);
                    postEntity.setPostState(PostState.PLANNED);
                }
            }else{
                postEntity=convertToEntity(postDto);
                postEntity.setPostState(PostState.SAVED);
            }
                 //Устанавливаем юзера к посту, и добавляем пост к юзеру
            userOptional.get().addPost(postEntity);
            PostEntity savedPost = postRepository.save(postEntity);
            if(savedPost.getPostState()==PostState.PLANNED){
                PostSenderTask postSenderTask = new PostSenderTask(bot, convertToPost(savedPost), this, postSender);
                log.info("Пост № {} запланирован к отправке на {}", postDto.getPostId(), postDto.getDate());
                new Timer().schedule(postSenderTask, postDto.getDate());
            }
        }
    }

    public void addChat(PostEntity post, Chat chat){
        Optional<ChatEntity> entity = chatRepository.findByChatId(chat.getId().toString());
        entity.ifPresent(post::addChat);
    }

    public List<Post> todayPosts(Date dateUntil, PostState postState){
        return postRepository.findByDateBeforeAndPostState(dateUntil, postState).stream()
                .map(this::convertToPost).toList();
    }

    public List<Post> getPostsByState(PostState postState){
        return postRepository.findByPostState(postState).stream()
                .map(this::convertToPost).toList();
    }

    @Transactional
    //Изменение статуса
    public void savePost(Post post){
        Optional<PostEntity> postEntity = postRepository.findById(post.getPostId());
        if(postEntity.isPresent()){
            PostEntity get = postEntity.get();
            get.setPostState(PostState.SENT);
            postRepository.save(get);
        }
    }
    public List<Post> findByUserAndPostStates(User user, List<PostState> postStates){
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(user.getId().toString());
        return postRepository.findByUserAndPostStateIn(
                userEntity,postStates).stream().map(this::convertToPost).toList();
    }

    public List<PostEntity> findByUser(User user){
        UserEntity userEntity = new UserEntity();
        userEntity.setUserId(user.getId().toString());
        return postRepository.findAllByUser(userEntity);
    }

    @Transactional
    public void deleteById(Integer id){
        postRepository.deleteById(id);
    }

    public Optional<Post> findById(Integer id) {
        return postRepository.findById(id).map(this::convertToPost);
    }
    public boolean existsByPostId(Integer id){
        return postRepository.existsByPostId(id);
    }
////////////////////////////////Конвертеры/////////////////////
    private PostEntity convertToEntity(Post post){
        PostEntity postEntity = new PostEntity();
        if(post.getPostId()!=0){
            postEntity.setPostId(post.getPostId());}
        for(var text: post.getTexts()){
            postEntity.addText(new TextEntity(text));
        }
        for(var audio: post.getAudios()){
            AudioEntity audioEntity = new AudioEntity(audio.getFileId(), audio.getFileName());
            if(post.getCaption().containsKey(audio.getFileId())){
                audioEntity.setCaption(post.getCaption().get(audio.getFileId()));
            }
            postEntity.addAudio(audioEntity);
        }
        for(var photo: post.getPhotos()){
            PhotoEntity photoEntity = new PhotoEntity(photo.getFileId(),photo.getFileSize());
            if(post.getCaption().containsKey(photo.getFileId())){
                photoEntity.setCaption(post.getCaption().get(photo.getFileId()));
            }
            postEntity.addPhoto(photoEntity);
        }
        for (var doc: post.getDocs()){
            DocEntity docEntity = new DocEntity(doc.getFileId(),doc.getFileName());
            if(post.getCaption().containsKey(doc.getFileId())){
                docEntity.setCaption(post.getCaption().get(doc.getFileId()));
            }
            postEntity.addDoc(docEntity);
        }
        for(var chat: post.getChats()){
            addChat(postEntity,chat);
        }
        for (var poll: post.getPolls()){
            PollEntity pollEntity = new PollEntity();
            pollEntity.setQuestion(poll.getQuestion());
            pollEntity.setMultipleAnswer(poll.getAllowMultipleAnswers());
            List<PollOption> options = poll.getOptions();
            pollEntity.setPollOptions(options.stream().map(PollOption::getText).collect(Collectors.toList()));
            pollEntity.setAnonymous(poll.getIsAnonymous());
            pollEntity.setType(poll.getType());
            if(poll.getType().equalsIgnoreCase("quiz")){
                pollEntity.setExplanation(poll.getExplanation()==null?"":poll.getExplanation());
                List<MessageEntity> explanationEntities = poll.getExplanationEntities()==null?Collections.emptyList()
                        :poll.getExplanationEntities();
                pollEntity.setExplTexts(explanationEntities.stream().map(MessageEntity::getText)
                        .collect(Collectors.toList()));
                pollEntity.setCorrectOptionId(poll.getCorrectOptionId());
            }
            postEntity.addPoll(pollEntity);
        }
        for (var video :post.getVideos()){
            VideoEntity videoEntity = new VideoEntity(video.getFileId(), video.getFileName());
            if(post.getCaption().containsKey(video.getFileId())){
                videoEntity.setCaption(post.getCaption().get(video.getFileId()));
            }
            postEntity.addVideo(videoEntity);
        }
        postEntity.setDate(post.getDate());
        postEntity.setPostState(post.getPostState());
        return postEntity;
    }
    private Post convertToPost(PostEntity pe){
        Post post = new Post();
        post.setPostId(pe.getPostId());
        post.setUser(userService.entityToUser(pe.getUser()));
        post.setPostState(pe.getPostState());
        post.setPostId(pe.getPostId());
        post.setDate(pe.getDate());
        post.setChats(pe.getChats().stream().map(userService::entityToChat).collect(Collectors.toSet()));
        for(var a: pe.getAudios()){
            if(a.getCaption()!=null){
                post.getCaption().put(entityToAudio(a).getFileId(),a.getCaption());
            }
            post.getAudios().add(entityToAudio(a));
        }
        for(var d: pe.getDocs()){
            if(d.getCaption()!=null){
                post.getCaption().put(entityToDocument(d).getFileId(),d.getCaption());
            }
            post.getDocs().add(entityToDocument(d));
        }
        for(var p: pe.getPhotos()){
            if(p.getCaption()!=null){
                post.getCaption().put(entityToPhotoSize(p).getFileId(),p.getCaption());
            }
            post.getPhotos().add(entityToPhotoSize(p));
        }

        post.setTexts(pe.getTexts().stream().map(TextEntity::getText).collect(Collectors.toList()));
        post.setVideos(pe.getVideos().stream().map(this::entityToVideo).collect(Collectors.toList()));
        for(var v: pe.getVideos()){
            if(v.getCaption()!=null){
                post.getCaption().put(entityToVideo(v).getFileId(),v.getCaption());
            }
            post.getVideos().add(entityToVideo(v));
        }
        post.setPolls(pe.getPolls().stream().map(this::entityToPoll).collect(Collectors.toList()));
        return post;
    }

    private Audio entityToAudio(AudioEntity e){
        Audio audio = new Audio();
        audio.setFileId(e.getAudioId());
        audio.setFileName(e.getName());
        return audio;
    }

    private Video entityToVideo(VideoEntity e){
        Video video = new Video();
        video.setFileId(e.getVideoId());
        video.setFileName(e.getName());
        return video;
    }

    private Document entityToDocument(DocEntity e){
        Document document = new Document();
        document.setFileId(e.getDocId());
        document.setFileName(e.getDocName());
        return document;
    }

    private PhotoSize entityToPhotoSize(PhotoEntity e){
        PhotoSize photoSize = new PhotoSize();
        photoSize.setFileId(e.getPhoto_id());
        photoSize.setFileSize(e.getSize());
        return photoSize;
    }
    private Poll entityToPoll(PollEntity e){
        Poll poll = new Poll();
        poll.setId(String.valueOf(e.getPollId()));
        poll.setQuestion(e.getQuestion());
        poll.setAllowMultipleAnswers(e.isMultipleAnswer());
        List<String> options = e.getPollOptions();
        poll.setOptions(options.stream().map(
                x->{
                    PollOption po = new PollOption();
                    po.setText(x);
                    return po;
                }
        ).collect(Collectors.toList()));
        poll.setIsAnonymous(e.isAnonymous());
        poll.setType(e.getType());
        if(e.getType().equalsIgnoreCase("quiz")){
            poll.setExplanation(e.getExplanation());
            poll.setExplanationEntities(e.getExplTexts().stream().map(
                    x->{
                        MessageEntity me = new MessageEntity();
                        me.setText(x);
                        return me;
                    }
            ).collect(Collectors.toList()));
            poll.setCorrectOptionId(e.getCorrectOptionId());
        }
        return poll;
    }


}
