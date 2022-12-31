package ru.veselov.plannerBot.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.veselov.plannerBot.model.content.*;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "post")
@Getter
@Setter
@EqualsAndHashCode(exclude = "postId")
@NoArgsConstructor
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private int postId;

    @Column(name = "planned_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date date;


    ////////////
    @OneToMany(mappedBy = "post",
    cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageDBEntity> messages = new LinkedList<>();

    ///////////
    @OneToMany(mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<PhotoEntity> photos=new LinkedList<>();

    @OneToMany(mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<AudioEntity> audios=new LinkedList<>();

    @OneToMany(mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<VideoEntity> videos=new LinkedList<>();

    @OneToMany(mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<DocEntity> docs=new LinkedList<>();

    @OneToMany(mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<PollEntity> polls= new LinkedList<>();

    @Enumerated(EnumType.STRING)
    PostState postState=PostState.CREATED;


    @ManyToMany(cascade = {CascadeType.MERGE,CascadeType.REFRESH})
    @JoinTable(
            name = "post_chat",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "chat_id"))
    private Set<ChatEntity> chats= new HashSet<>();
    //Bidirectional relationship

    public void addChat(ChatEntity chat){
        this.chats.add(chat);
        chat.getPosts().add(this);
    }
    public void removeChat(ChatEntity chat){
        this.chats.remove(chat);
        chat.getPosts().remove(this);
    }

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserEntity user;

    public void setUser(UserEntity user){
        this.user=user;
    }


    ////////////
    public void addMessage(MessageDBEntity messageDB){
        messageDB.setPost(this);
        this.messages.add(messageDB);
    }
    ////////////

    public void addPhoto(PhotoEntity photo) {
        photo.setPost(this);
        this.photos.add(photo);
    }

    public void addAudio(AudioEntity audio) {
        audio.setPost(this);
        audios.add(audio);
    }
    public void addVideo(VideoEntity video) {
        video.setPost(this);
        videos.add(video);
    }

    public void addDoc(DocEntity doc) {
        doc.setPost(this);
        docs.add(doc);
    }

    public void addPoll(PollEntity poll) {
        poll.setPost(this);
        polls.add(poll);
    }
}
