package ru.veselov.plannerBot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.polls.Poll;

import java.util.*;
//DTO
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(exclude = "postId")
public class Post {

    private int postId;

    private Date date;

    private List<String> texts=new LinkedList<>();

    private List<PhotoSize> photos=new LinkedList<>();
    private Map<String, String> caption = new HashMap<>();

    private List<Audio> audios=new LinkedList<>();
    private List<Document> docs=new LinkedList<>();
    private List<Video> videos = new LinkedList<>();

    private List<Poll> polls= new LinkedList<>();

    private PostState postState=PostState.CREATED;

    private Set<Chat> chats= new HashSet<>();

    private User user;
}
