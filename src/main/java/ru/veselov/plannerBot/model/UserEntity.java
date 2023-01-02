package ru.veselov.plannerBot.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;
import ru.veselov.plannerBot.model.content.ChatEntity;

import javax.persistence.*;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name="user_id")
    private String userId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "username")
    private String userName;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private UserStatus status=UserStatus.STANDARD;
    //TODO один чат должен быть только у одного юзера, сделать такое ограничение
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE,CascadeType.REFRESH})
    @JoinTable(
            name = "user_chat",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "chat_id"))
    private Set<ChatEntity> chats=new HashSet<>();
    //Bidirectional relationship
    public void addChat(ChatEntity chat){
        this.chats.add(chat);
        chat.getUsers().add(this);
    }
    //Bidirectional relationship
    public void removeChat(ChatEntity chat){
        this.chats.remove(chat);
        chat.getUsers().remove(this);
    }

    @OneToMany(
            mappedBy = "user",
            cascade = {CascadeType.ALL},
            orphanRemoval = true
    )
    private List<PostEntity> posts=new LinkedList<>();
    public void addPost(PostEntity post){
        this.posts.add(post);
        post.setUser(this);
    }
    public void removePost(PostEntity post){
        this.posts.remove(post);
        post.setUser(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        UserEntity that = (UserEntity) o;
        return userId != null && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
